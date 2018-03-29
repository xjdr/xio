package com.xjeffrose.xio.tracing;

import io.netty.channel.*;
import io.netty.handler.codec.http.HttpRequest;

class HttpClientRequestTracingHandler extends ChannelOutboundHandlerAdapter {

  private final HttpClientTracingState state;

  public HttpClientRequestTracingHandler(HttpClientTracingState state) {
    super();
    this.state = state;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (!(msg instanceof HttpRequest)) {
      ctx.write(msg, promise);
      return;
    }

    HttpRequest request = (HttpRequest) msg;
    state.onRequest(ctx, request);

    ctx.write(msg, promise)
        .addListener(
            new ChannelFutureListener() {
              @Override
              public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                  state.onError(ctx, future.cause());
                }
              }
            });
  }
}
