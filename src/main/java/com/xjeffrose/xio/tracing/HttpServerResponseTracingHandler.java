package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.Tracer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;

class HttpServerResponseTracingHandler extends ChannelOutboundHandlerAdapter {

  private final HttpServerTracingState state;

  public HttpServerResponseTracingHandler(HttpServerTracingState state) {
    super();
    this.state = state;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (!(msg instanceof HttpResponse)) {
      ctx.write(msg, promise);
      return;
    }

    HttpResponse response = (HttpResponse) msg;
    Span span = state.responseSpan(ctx);

    Throwable error = null;
    try (Tracer.SpanInScope ws = state.responseSpanInScope(ctx)) {
      ws.close();
      ctx.write(msg, promise);
    } catch (Exception | Error e) {
      error = e;
      throw e;
    } finally {
      state.getHandler().handleSend(response, error, span);
    }
  }

}
