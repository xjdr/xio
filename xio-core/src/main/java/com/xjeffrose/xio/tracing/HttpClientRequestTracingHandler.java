package com.xjeffrose.xio.tracing;

import com.xjeffrose.xio.http.Request;
import io.netty.channel.*;

class HttpClientRequestTracingHandler extends ChannelOutboundHandlerAdapter {

  private final HttpClientTracingDispatch state;

  public HttpClientRequestTracingHandler(HttpClientTracingDispatch state) {
    super();
    this.state = state;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (!(msg instanceof Request)) {
      ctx.write(msg, promise);
      return;
    }
    Request request = (Request) msg;

    if (request.startOfStream()) {
      state.onRequest(ctx, request);
    }
    ctx.write(msg, promise)
        .addListener(
            future -> {
              if (!future.isSuccess()) {
                state.onError(ctx, request, future.cause());
              }
            });
  }
}
