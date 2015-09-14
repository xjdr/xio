package com.xjeffrose.xio.clientBak;


import io.airlift.log.Logger;
import io.airlift.units.Duration;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public abstract class AbstractClientChannel extends SimpleChannelInboundHandler<Object> implements XioClientChannel {
  private static final Logger LOGGER = Logger.get(AbstractClientChannel.class);

  private final Channel nettyChannel;
  private final Map<Integer, Request> requestMap = new HashMap<>();
  private final Timer timer;
  private final XioProtocolFactory protocolFactory;
  private Duration sendTimeout = null;
  // Timeout until the whole request must be received.
  private Duration receiveTimeout = null;
  // Timeout for not receiving any data from the server
  private Duration readTimeout = null;
  private volatile XioException channelError;
  private ChannelHandlerContext ctx;

  protected AbstractClientChannel(Channel nettyChannel, Timer timer, XioProtocolFactory protocolFactory) {
    this.nettyChannel = nettyChannel;
    this.timer = timer;
    this.protocolFactory = protocolFactory;
  }

  @Override
  public Channel getNettyChannel() {
    return nettyChannel;
  }

  @Override
  public ChannelHandlerContext getCtx() {
    return ctx;
  }

  @Override
  public XioProtocolFactory getProtocolFactory() {
    return protocolFactory;
  }

  protected int extractSequenceId(ByteBuf messageBuffer) throws XioTransportException {
    try {
      //TODO: REMOVE THIS
      return 1;
    } catch (Throwable t) {
      throw new XioTransportException("Could not find sequenceId in message");
    }
  }

  protected ByteBuf extractResponse(Object message) throws XioTransportException {
    if (message == null) {
      throw new XioTransportException("Response was null");
    }
    return (ByteBuf) message;
  }

  protected abstract ChannelFuture writeRequest(ByteBuf request);

  public void close() {
    getNettyChannel().close();
  }

  @Override
  public Duration getSendTimeout() {
    return sendTimeout;
  }

  @Override
  public void setSendTimeout(@Nullable Duration sendTimeout) {
    this.sendTimeout = sendTimeout;
  }

  @Override
  public Duration getReceiveTimeout() {
    return receiveTimeout;
  }

  @Override
  public void setReceiveTimeout(@Nullable Duration receiveTimeout) {
    this.receiveTimeout = receiveTimeout;
  }

  @Override
  public Duration getReadTimeout() {
    return this.readTimeout;
  }

  @Override
  public void setReadTimeout(@Nullable Duration readTimeout) {
    this.readTimeout = readTimeout;
  }

  @Override
  public boolean hasError() {
    return channelError != null;
  }

  @Override
  public XioException getError() {
    return channelError;
  }

  @Override
  public void executeInIoThread(Runnable runnable) {
    getNettyChannel().eventLoop().execute(runnable);
  }

  @Override
  public void sendAsynchronousRequest(final ByteBuf message,
                                      final boolean oneway,
                                      final Listener listener) throws XioException {
    final int sequenceId = extractSequenceId(message);

    // Ensure channel listeners are always called on the channel's I/O thread
    executeInIoThread(new Runnable() {
      @Override
      public void run() {
        try {
          final Request request = makeRequest(sequenceId, listener);

          if (!nettyChannel.isActive()) {
            fireChannelErrorCallback(listener, new XioTransportException("Channel closed")); //NOT_OPEN
            return;
          }

          if (hasError()) {
            fireChannelErrorCallback(
                listener,
                new XioTransportException("Channel is in a bad state due to failing a previous request")); //UNKNOWN
            return;
          }

          ChannelFuture sendFuture = writeRequest(message);
          queueSendTimeout(request);

          sendFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
              messageSent(future, request, oneway);
            }
          });
        } catch (Throwable t) {
          // onError calls all registered listeners in the requestMap, but this request
          // may not be registered yet. So we try to remove it (to make sure we don't call
          // the callback twice) and then manually make the callback for this request
          // listener.
          requestMap.remove(sequenceId);
          fireChannelErrorCallback(listener, t);

          onError(t);
        }
      }
    });
  }

  private void messageSent(ChannelFuture future, Request request, boolean oneway) {
    try {
      if (future.isSuccess()) {
        cancelRequestTimeouts(request);
        fireRequestSentCallback(request.getListener());
        if (oneway) {
          retireRequest(request);
        } else {
          queueReceiveAndReadTimeout(request);
        }
      } else {
        XioTransportException transportException =
            new XioTransportException("Sending request failed",
                future.cause());
        onError(transportException);
      }
    } catch (Throwable t) {
      onError(t);
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    this.ctx = ctx;

    try {
      ByteBuf response = extractResponse(msg);

      if (response != null) {
        int sequenceId = extractSequenceId(response);
        onResponseReceived(sequenceId, response);
      } else {
        ctx.fireChannelRead(msg);
      }
    } catch (Throwable t) {
      onError(t);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    onError(cause);
  }

  private Request makeRequest(int sequenceId, Listener listener) {
    Request request = new Request(listener);
    requestMap.put(sequenceId, request);
    return request;
  }

  private void retireRequest(Request request) {
    cancelRequestTimeouts(request);
  }

  private void cancelRequestTimeouts(Request request) {
    Timeout sendTimeout = request.getSendTimeout();
    if (sendTimeout != null && !sendTimeout.isCancelled()) {
      sendTimeout.cancel();
    }

    Timeout receiveTimeout = request.getReceiveTimeout();
    if (receiveTimeout != null && !receiveTimeout.isCancelled()) {
      receiveTimeout.cancel();
    }

    Timeout readTimeout = request.getReadTimeout();
    if (readTimeout != null && !readTimeout.isCancelled()) {
      readTimeout.cancel();
    }
  }

  private void cancelAllTimeouts() {
    for (Request request : requestMap.values()) {
      cancelRequestTimeouts(request);
    }
  }

  private void onResponseReceived(int sequenceId, ByteBuf response) {
    Request request = requestMap.remove(sequenceId);
    if (request == null) {
      onError(new XioTransportException("Bad sequence id in response: " + sequenceId));
    } else {
      retireRequest(request);
      fireResponseReceivedCallback(request.getListener(), response);
    }
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    if (!requestMap.isEmpty()) {
      onError(new XioTransportException("Client was disconnected by server"));
    }
  }

  protected void onError(Throwable t) {
    XioException wrappedException = wrapException(t);

    if (channelError == null) {
      channelError = wrappedException;
    }

    cancelAllTimeouts();

    Collection<Request> requests = new ArrayList<>();
    requests.addAll(requestMap.values());
    requestMap.clear();
    for (Request request : requests) {
      fireChannelErrorCallback(request.getListener(), wrappedException);
    }

    Channel channel = getNettyChannel();
    if (nettyChannel.isOpen()) {
      ctx.close();
      channel.close();
    }
  }

  protected XioException wrapException(Throwable t) {
    if (t instanceof XioException) {
      return (XioException) t;
    } else {
      return new XioTransportException(t);
    }
  }

  private void fireRequestSentCallback(Listener listener) {
    try {
      listener.onRequestSent();
    } catch (Throwable t) {
      LOGGER.warn(t, "Request sent listener callback triggered an exception");
    }
  }

  private void fireResponseReceivedCallback(Listener listener, ByteBuf response) {
    try {
      listener.onResponseReceived(response);
    } catch (Throwable t) {
      LOGGER.warn(t, "Response received listener callback triggered an exception");
    }
  }

  private void fireChannelErrorCallback(Listener listener, XioException exception) {
    try {
      listener.onChannelError(exception);
    } catch (Throwable t) {
      LOGGER.warn(t, "Channel error listener callback triggered an exception");
    }
  }

  private void fireChannelErrorCallback(Listener listener, Throwable throwable) {
    fireChannelErrorCallback(listener, wrapException(throwable));
  }

  private void onSendTimeoutFired(Request request) {
    cancelAllTimeouts();
//    WriteTimeoutException timeoutException = new WriteTimeoutException("Timed out waiting " + getSendTimeout() + " to send data to server");
    WriteTimeoutException timeoutException = WriteTimeoutException.INSTANCE;
    fireChannelErrorCallback(request.getListener(), new XioTransportException("Timed out", timeoutException)); //TIMED_OUT
  }

  private void onReceiveTimeoutFired(Request request) {
    cancelAllTimeouts();
//    ReadTimeoutException timeoutException = new ReadTimeoutException("Timed out waiting " + getReceiveTimeout() + " to receive response");
    ReadTimeoutException timeoutException = ReadTimeoutException.INSTANCE;
    fireChannelErrorCallback(request.getListener(), new XioTransportException("Timed out", timeoutException)); //TIMED_OUT
  }

  private void onReadTimeoutFired(Request request) {
    cancelAllTimeouts();
//    ReadTimeoutException timeoutException = new ReadTimeoutException("Timed out waiting " + getReadTimeout() + " to read data from server");
    ReadTimeoutException timeoutException = ReadTimeoutException.INSTANCE;
    fireChannelErrorCallback(request.getListener(), new XioTransportException("Timed out", timeoutException)); //TIMED_OUT
  }


  private void queueSendTimeout(final Request request) throws XioTransportException {
    if (this.sendTimeout != null) {
      long sendTimeoutMs = this.sendTimeout.toMillis();
      if (sendTimeoutMs > 0) {
        TimerTask sendTimeoutTask = new IoThreadBoundTimerTask(this, new TimerTask() {
          @Override
          public void run(Timeout timeout) {
            onSendTimeoutFired(request);
          }
        });

        Timeout sendTimeout;
        try {
          sendTimeout = timer.newTimeout(sendTimeoutTask, sendTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (IllegalStateException e) {
          throw new XioTransportException("Unable to schedule send timeout");
        }
        request.setSendTimeout(sendTimeout);
      }
    }
  }

  private void queueReceiveAndReadTimeout(final Request request) throws XioTransportException {
    if (this.receiveTimeout != null) {
      long receiveTimeoutMs = this.receiveTimeout.toMillis();
      if (receiveTimeoutMs > 0) {
        TimerTask receiveTimeoutTask = new IoThreadBoundTimerTask(this, new TimerTask() {
          @Override
          public void run(Timeout timeout) {
            onReceiveTimeoutFired(request);
          }
        });

        Timeout timeout;
        try {
          timeout = timer.newTimeout(receiveTimeoutTask, receiveTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (IllegalStateException e) {
          throw new XioTransportException("Unable to schedule request timeout");
        }
        request.setReceiveTimeout(timeout);
      }
    }

    if (this.readTimeout != null) {
      long readTimeoutNanos = this.readTimeout.roundTo(TimeUnit.NANOSECONDS);
      if (readTimeoutNanos > 0) {
        TimerTask readTimeoutTask = new IoThreadBoundTimerTask(this, new ReadTimeoutTask(readTimeoutNanos, request));

        Timeout timeout;
        try {
          timeout = timer.newTimeout(readTimeoutTask, readTimeoutNanos, TimeUnit.NANOSECONDS);
        } catch (IllegalStateException e) {
          throw new XioTransportException("Unable to schedule read timeout");
        }
        request.setReadTimeout(timeout);
      }
    }
  }


  /**
   * Used to create TimerTasks that will fire
   */
  private static class IoThreadBoundTimerTask implements TimerTask {
    private final XioClientChannel channel;
    private final TimerTask timerTask;

    public IoThreadBoundTimerTask(XioClientChannel channel, TimerTask timerTask) {
      this.channel = channel;
      this.timerTask = timerTask;
    }

    @Override
    public void run(final Timeout timeout)
        throws Exception {
      channel.executeInIoThread(new Runnable() {
        @Override
        public void run() {
          try {
            timerTask.run(timeout);
          } catch (Exception e) {
            channel.getCtx().fireExceptionCaught(e);
          }
        }
      });
    }
  }

  /**
   * Bundles the details of a client request that has started, but for which a response hasn't yet
   * been received (or in the one-way case, the send operation hasn't completed yet).
   */
  private static class Request {
    private final Listener listener;
    private Timeout sendTimeout;
    private Timeout receiveTimeout;

    private volatile Timeout readTimeout;

    public Request(Listener listener) {
      this.listener = listener;
    }

    public Listener getListener() {
      return listener;
    }

    public Timeout getReceiveTimeout() {
      return receiveTimeout;
    }

    public void setReceiveTimeout(Timeout receiveTimeout) {
      this.receiveTimeout = receiveTimeout;
    }

    public Timeout getReadTimeout() {
      return readTimeout;
    }

    public void setReadTimeout(Timeout readTimeout) {
      this.readTimeout = readTimeout;
    }

    public Timeout getSendTimeout() {
      return sendTimeout;
    }

    public void setSendTimeout(Timeout sendTimeout) {
      this.sendTimeout = sendTimeout;
    }
  }

  private final class ReadTimeoutTask implements TimerTask {
    private final TimeoutHandler timeoutHandler;
    private final long timeoutNanos;
    private final Request request;

    ReadTimeoutTask(long timeoutNanos, Request request) {
      this.timeoutHandler = TimeoutHandler.findTimeoutHandler(getNettyChannel().pipeline());
      this.timeoutNanos = timeoutNanos;
      this.request = request;
    }

    public void run(Timeout timeout) throws Exception {
      if (timeoutHandler == null) {
        return;
      }

      if (timeout.isCancelled()) {
        return;
      }

      if (!getNettyChannel().isOpen()) {
        return;
      }

      long currentTimeNanos = System.nanoTime();

      long timePassed = currentTimeNanos - timeoutHandler.getLastMessageReceivedNanos();
      long nextDelayNanos = timeoutNanos - timePassed;

      if (nextDelayNanos <= 0) {
        onReadTimeoutFired(request);
      } else {
        request.setReadTimeout(timer.newTimeout(this, nextDelayNanos, TimeUnit.NANOSECONDS));
      }
    }
  }
}
