package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.Tracer;
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
    Span span = state.requestSpan(ctx, request);

    try (Tracer.SpanInScope ws = state.requestSpanInScope(ctx, span)) {
      ctx.fireChannelRead(request);
    } catch (Exception | Error e) {
      System.out.println("Caught Exception: " + e);
      throw e;
    }
  }

}
