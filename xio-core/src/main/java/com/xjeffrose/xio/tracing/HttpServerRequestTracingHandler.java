package com.xjeffrose.xio.tracing;

import com.xjeffrose.xio.http.Request;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

class HttpServerRequestTracingHandler extends SimpleChannelInboundHandler<Request> {

  private final HttpServerTracingDispatch state;

  public HttpServerRequestTracingHandler(HttpServerTracingDispatch state) {
    super();
    this.state = state;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
    if (request.startOfStream()) {
      state.onRequest(ctx, request);
    }
    ctx.fireChannelRead(request);
  }

  // Don't need to override exceptionCaught since we don't have a request to create a span from.

}
