package com.xjeffrose.xio.server;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.core.ConnectionContext;
import com.xjeffrose.xio.core.ConnectionContexts;
import com.xjeffrose.xio.processor.XioProcessor;
import com.xjeffrose.xio.processor.XioProcessorFactory;
import com.xjeffrose.xio.processor.XioSimpleProcessor;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCounted;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;


public class XioDispatcher extends ChannelInboundHandlerAdapter {
  private static final Logger log = Logger.getLogger(XioServerTransport.class.getName());

  private final XioProcessorFactory processorFactory;
  private final long taskTimeoutMillis;
  private final Timer taskTimeoutTimer;
  private final int queuedResponseLimit;
  private final Map<Integer, Object> responseMap = new HashMap<>();
  private final AtomicInteger dispatcherSequenceId = new AtomicInteger(0);
  private final AtomicInteger lastResponseWrittenId = new AtomicInteger(0);
  private final long requestStart;
  private XioProcessor processor;

  private RequestContext requestContext;
  private boolean isOrderedResponsesRequired = true;
  private XioServerConfig config;

  public XioDispatcher(XioServerDef def, XioServerConfig config) {
    this.config = config;
    this.processorFactory = def.getProcessorFactory();
    this.queuedResponseLimit = def.getQueuedResponseLimit();
    this.taskTimeoutMillis = (def.getTaskTimeout() == null ? 0 : def.getTaskTimeout().toMillis());
    this.taskTimeoutTimer = (def.getTaskTimeout() == null ? null : config.getTimer());
    this.requestStart = System.currentTimeMillis();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    // Any out of band exception are caught here and we tear down the socket
    closeChannel(ctx);

    // Send for logging
    ctx.fireExceptionCaught(cause);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    // Reads always start out unblocked
    DispatcherContext.unblockChannelReads(ctx);
    this.processor = processorFactory.getProcessor();
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (processor != null) {
      processor.disconnect(ctx);
    }
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
    DispatcherContext.blockChannelReads(ctx);

    if (processor instanceof XioSimpleProcessor) {
      // for proxy case, really no need to go through all the complexity in processRequest
      processor.process(ctx, o, requestContext);
    } else {
      processRequest(ctx, o);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//    DispatcherContext.blockChannelReads(ctx);

    ctx.fireChannelReadComplete();
  }

  private void processRequest(
      final ChannelHandlerContext ctx,
      final Object message) {

    final int requestSequenceId = dispatcherSequenceId.incrementAndGet();

    synchronized (responseMap) {
      // Limit the number of pending responses (responses which finished out of order, and are
      // waiting for previous requests to be finished so they can be written in order), by
      // blocking further channel reads. Due to the way Netty frame decoders work, this is more
      // of an estimate than a hard limit. Netty may continue to decode and process several
      // more requests that were in the latest read, even while further reads on the channel
      // have been blocked.
      if (requestSequenceId > lastResponseWrittenId.get() + queuedResponseLimit &&
          !DispatcherContext.isChannelReadBlocked(ctx)) {
        DispatcherContext.blockChannelReads(ctx);
      }
    }

    try {
      ctx.executor().execute(() -> {
        ListenableFuture<Boolean> processFuture;
        final AtomicBoolean responseSent = new AtomicBoolean(false);
        // Use AtomicReference as a generic holder class to be able to mark it final
        // and pass into inner classes. Since we only use .get() and .set(), we don't
        // actually do any atomic operations.
        final AtomicReference<Timeout> expireTimeout = new AtomicReference<>(null);

        try {
          try {
            long timeRemaining = 0;
            if (taskTimeoutMillis > 0) {
              long timeElapsed = System.currentTimeMillis() - requestStart;
              if (timeElapsed >= taskTimeoutMillis) {
                // TODO(JR): Send (Throw?) timeout exception
                sendApplicationException(HttpResponseStatus.REQUEST_TIMEOUT, ctx);
                return;
              } else {
                timeRemaining = taskTimeoutMillis - timeElapsed;
              }
            }

            if (timeRemaining > 0) {
              expireTimeout.set(taskTimeoutTimer.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                  if (responseSent.compareAndSet(false, true)) {
                    sendApplicationException(HttpResponseStatus.REQUEST_TIMEOUT, ctx);
                    // TODO(JR): Send (Throw?) timeout exception
                  }
                }
              }, timeRemaining, TimeUnit.MILLISECONDS));
            }

            ConnectionContext connectionContext = ConnectionContexts.getContext(ctx.channel());
            requestContext = new XioRequestContext(connectionContext);
            RequestContexts.setCurrentContext(requestContext);

            processFuture = processor.process(ctx, message, requestContext);
          } finally {
            // RequestContext does NOT stay set while we are waiting for the process
            // future to complete. This is by design because we'll might move on to the
            // next request using this thread before this one is completed. If you need
            // the context throughout an asynchronous handler, you need to read and store
            // it before returning a future.
            RequestContexts.clearCurrentContext();
          }

          Futures.addCallback(
              processFuture,
              new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                  deleteExpirationTimer(expireTimeout.get());

                  Object response = requestContext.getContextData(requestContext.getConnectionId());
                  // passing through case
                  if (response == null) {
                    return;
                  }

                  try {
                    // Only write response if the client is still there and the task timeout
                    // hasn't expired.
                    if (ctx.channel().isActive() && responseSent.compareAndSet(false, true)) {
                      writeResponse(ctx, response, requestSequenceId);
                    }
                  } catch (Throwable t) {
                    onDispatchException(ctx, t);
                  }
                }

                @Override
                public void onFailure(Throwable t) {
                  deleteExpirationTimer(expireTimeout.get());
                  onDispatchException(ctx, t);
                }
              }
          );
        } catch (Exception e) {
          onDispatchException(ctx, e);
        }
      });
    } catch (RejectedExecutionException ex) {
      sendApplicationException(HttpResponseStatus.INTERNAL_SERVER_ERROR, ctx);
    }
  }

  private void deleteExpirationTimer(Timeout timeout) {
    if (timeout == null) {
      return;
    }
    timeout.cancel();
  }

  private void sendApplicationException(HttpResponseStatus status, ChannelHandlerContext ctx) {
    RequestContext reqCtx = new XioRequestContext(new XioConnectionContext());
    reqCtx.setContextData(reqCtx.getConnectionId(), new DefaultHttpResponse(HttpVersion.HTTP_1_1, status));
    writeResponse(ctx, reqCtx.getContextData(reqCtx.getConnectionId()), dispatcherSequenceId.get());

  }

  private void onDispatchException(ChannelHandlerContext ctx, Throwable t) {
    ctx.fireExceptionCaught(t);
    closeChannel(ctx);
  }

  private void writeResponse(ChannelHandlerContext ctx,
                             Object response,
                             int responseSequenceId) {

    if (isOrderedResponsesRequired) {
      writeResponseInOrder(ctx, response, responseSequenceId);
    } else {
      // No ordering required, just write the response immediately
      if (response != null) {
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        lastResponseWrittenId.incrementAndGet();
      } else {
        //TODO(JR): Do something
        log.error("------------------Would have returned null response ------------------");
      }
    }
  }

  private void writeResponseInOrder(ChannelHandlerContext ctx,
                                    Object response,
                                    int responseSequenceId) {
    // Ensure responses to requests are written in the same order the requests
    // were received.
    synchronized (responseMap) {
      int currentResponseId = lastResponseWrittenId.get() + 1;
      if (responseSequenceId != currentResponseId) {
        // This response is NOT next in line of ordered responses, save it to
        // be sent later, after responses to all earlier requests have been
        // sent.
        responseMap.put(responseSequenceId, response);
      } else {
        // This response was next in line, write this response now, and see if
        // there are others next in line that should be sent now as well.
        do {
//          ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
          ctx.writeAndFlush(response);
          lastResponseWrittenId.incrementAndGet();
          ++currentResponseId;
          response = responseMap.remove(currentResponseId);
        } while (null != response);

        // Now that we've written some responses, check if reads should be unblocked
        if (DispatcherContext.isChannelReadBlocked(ctx)) {
          int lastRequestSequenceId = dispatcherSequenceId.get();
          if (lastRequestSequenceId <= lastResponseWrittenId.get() + queuedResponseLimit) {
            DispatcherContext.unblockChannelReads(ctx);
          }
        }
      }
    }
  }

  private void closeChannel(ChannelHandlerContext ctx) {
    if (ctx.channel().isOpen()) {
      ctx.channel().close();
    }
  }

  private static class DispatcherContext {
    private ReadBlockedState readBlockedState = ReadBlockedState.NOT_BLOCKED;

    public static boolean isChannelReadBlocked(ChannelHandlerContext ctx) {
      return getDispatcherContext(ctx).readBlockedState == ReadBlockedState.BLOCKED;
    }

    public static void blockChannelReads(ChannelHandlerContext ctx) {
      // Remember that reads are blocked (there is no Channel.getReadable())
      getDispatcherContext(ctx).readBlockedState = ReadBlockedState.BLOCKED;

      // NOTE: this shuts down reads, but isn't a 100% guarantee we won't get any more messages.
      // It sets up the channel so that the polling loop will not report any new read events
      // and netty won't read any more data from the socket, but any messages already fully read
      // from the socket before this ran may still be decoded and arrive at this handler. Thus
      // the limit on queued messages before we block reads is more of a guidance than a hard
      // limit.
      ctx.channel().config().setAutoRead(false);
    }

    public static void unblockChannelReads(ChannelHandlerContext ctx) {
      // Remember that reads are unblocked (there is no Channel.getReadable())
      getDispatcherContext(ctx).readBlockedState = ReadBlockedState.NOT_BLOCKED;
      ctx.channel().config().setAutoRead(true);
    }

    private static DispatcherContext getDispatcherContext(ChannelHandlerContext ctx) {
      final AttributeKey<DispatcherContext> DISPATCHER_CONTEXT = AttributeKey.valueOf("DispatcherContext");

      DispatcherContext dispatcherContext;
      Object attachment = ctx.attr(DISPATCHER_CONTEXT).get();

      if (attachment == null) {
        // No context was added yet, add one
        dispatcherContext = new DispatcherContext();
        ctx.attr(DISPATCHER_CONTEXT).set(dispatcherContext);
      } else if (!(attachment instanceof DispatcherContext)) {
        // There was a context, but it was the wrong type. This should never happen.
        throw new IllegalStateException("XioDispatcher handler context should be of type XioDispatcher.DispatcherContext");
      } else {
        dispatcherContext = (DispatcherContext) attachment;
      }

      return dispatcherContext;
    }

    private enum ReadBlockedState {
      NOT_BLOCKED,
      BLOCKED,
    }
  }
}
