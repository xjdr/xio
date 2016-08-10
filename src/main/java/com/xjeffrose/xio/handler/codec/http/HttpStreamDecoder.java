package com.xjeffrose.xio.handler.codec.http;

import com.xjeffrose.xio.handler.util.ContextualMessageQueue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import java.util.List;

/**
 * Tries to aggregate an HttpObject stream to a single FullHttpRequest if possible, otherwise allows
 * the stream to pass through.
 */
public class HttpStreamDecoder extends HttpObjectAggregator {
  private static final FullHttpResponse CONTINUE =
    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE,
      Unpooled.EMPTY_BUFFER);

  private final ContextualMessageQueue<List<Object>, Object> messageQueue;

  public HttpStreamDecoder(int maxContentLength) {
    super(maxContentLength);
    messageQueue = new ContextualMessageQueue<>(List::add);
  }

  @Override
  protected Object newContinueResponse(HttpMessage httpMessage, int i, ChannelPipeline pipeline) {
    if (HttpUtil.is100ContinueExpected(httpMessage)) {
      return CONTINUE.retainedDuplicate();
    }
    return null;
  }

  @Override
  protected boolean closeAfterContinueResponse(Object o) {
    return false;
  }

  @Override
  protected boolean ignoreContentAfterContinueResponse(Object o) {
    return false;
  }

  @Override
  protected FullHttpMessage beginAggregation(HttpMessage httpMessage, ByteBuf byteBuf)
    throws Exception {
    final String transferEncoding = httpMessage.headers().get(HttpHeaderNames.TRANSFER_ENCODING);
    final FullHttpMessage fullMessage = super.beginAggregation(httpMessage, byteBuf);
    if (transferEncoding != null) {
      // Restore the transfer encoding here, we will remove 'chunked' in finishAggregation if needed.
      httpMessage.headers().set(HttpHeaderNames.TRANSFER_ENCODING, transferEncoding);
    }
    return fullMessage;
  }

  @Override
  protected void finishAggregation(FullHttpMessage aggregated) throws Exception {
    // We won't send any messages from the messageQueue, we will send the aggregated one instead
    resetQueue();
    HttpUtil.setTransferEncodingChunked(aggregated, false);
    super.finishAggregation(aggregated);
  }

  private void resetQueue() {
    // Need to release queued messages that won't be sent since they were retained in decode.
    messageQueue.forEachMessage(ReferenceCountUtil::release);
    messageQueue.reset();
  }

  @Override
  protected void handleOversizedMessage(ChannelHandlerContext ctx, HttpMessage oversized)
    throws Exception {
    messageQueue.startStreaming();
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
    throws Exception {
    ReferenceCountUtil.retain(msg);
    final boolean streaming = messageQueue.addContextualMessage(out, msg);
    if (!streaming) {
      super.decode(ctx, msg, out);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpObject && isStartMessage((HttpObject) msg)) {
      resetQueue();
    }
    super.channelRead(ctx, msg);
  }
}
