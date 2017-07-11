package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;

class HttpClientRequestTracingHandler extends ChannelOutboundHandlerAdapter {

  private final HttpClientTracingState state;

  public HttpClientRequestTracingHandler(HttpClientTracingState state) {
    super();
    this.state = state;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (!(msg instanceof HttpRequest)) {
      ctx.write(msg, promise);
      return;
    }

    HttpRequest request = (HttpRequest) msg;
    Span span = state.requestSpan(ctx, request);

    try (Tracer.SpanInScope ws = state.requestSpanInScope(ctx, span)) {
      ctx.write(msg, promise);
    } catch (Exception | Error e) {
      throw e;
    }
  }

}
