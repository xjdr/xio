package com.xjeffrose.xio.tracing;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponse;

class HttpClientResponseTracingHandler extends SimpleChannelInboundHandler<HttpResponse> {

  private final HttpClientTracingState state;

  public HttpClientResponseTracingHandler(HttpClientTracingState state) {
    super();
    this.state = state;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpResponse response) throws Exception {
    state.onResponse(ctx, response);
    ctx.fireChannelRead(response);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    state.onError(ctx, cause);
    ctx.fireExceptionCaught(cause);
  }
}
