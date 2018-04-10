package com.xjeffrose.xio.tracing;

import com.xjeffrose.xio.http.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

class HttpClientResponseTracingHandler extends SimpleChannelInboundHandler<Response> {

  private final HttpClientTracingDispatch state;

  public HttpClientResponseTracingHandler(HttpClientTracingDispatch state) {
    super();
    this.state = state;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {
    if (response.endOfStream()) {
      state.onResponse(ctx, response);
    }
    ctx.fireChannelRead(response);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    state.onError(ctx, cause);
    ctx.fireExceptionCaught(cause);
  }
}
