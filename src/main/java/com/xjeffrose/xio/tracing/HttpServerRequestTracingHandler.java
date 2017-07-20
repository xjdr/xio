package com.xjeffrose.xio.tracing;

import brave.Span;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;

class HttpServerRequestTracingHandler extends SimpleChannelInboundHandler<HttpRequest> {

  private final HttpServerTracingState state;

  public HttpServerRequestTracingHandler(HttpServerTracingState state) {
    super();
    this.state = state;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
    Span span = state.onRequest(ctx, request);
    ctx.fireChannelRead(request);
  }

  // Don't need to override exceptionCaught since we don't have a request to create a span from.

}
