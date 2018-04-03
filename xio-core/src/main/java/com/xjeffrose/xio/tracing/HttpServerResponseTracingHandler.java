package com.xjeffrose.xio.tracing;

import com.xjeffrose.xio.http.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

class HttpServerResponseTracingHandler extends ChannelOutboundHandlerAdapter {

  private final HttpServerTracingDispatch state;

  public HttpServerResponseTracingHandler(HttpServerTracingDispatch state) {
    super();
    this.state = state;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (!(msg instanceof Response)) {
      ctx.write(msg, promise);
      return;
    }

    Response response = (Response) msg;
    if (response.endOfStream()) {
      ctx.write(msg, promise).addListener(future -> state.onResponse(response, future.cause()));
    } else {
      ctx.write(msg, promise);
    }
  }
}
