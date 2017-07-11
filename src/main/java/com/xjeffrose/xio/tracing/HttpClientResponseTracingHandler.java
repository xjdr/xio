package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.Tracer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;

class HttpClientResponseTracingHandler extends SimpleChannelInboundHandler<HttpResponse> {

  private final HttpClientTracingState state;

  public HttpClientResponseTracingHandler(HttpClientTracingState state) {
    super();
    this.state = state;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpResponse response) throws Exception {
    //System.out.println("TRACE HANDLER response:" + response);
    Span span = state.responseSpan(ctx);

    Throwable error = null;
    try (Tracer.SpanInScope ws = state.responseSpanInScope(ctx)) {
      ws.close();
      ctx.fireChannelRead(response);
    } catch (Exception | Error e) {
      error = e;
      throw e;
    } finally {
      state.getHandler().handleReceive(response, error, span);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    Span span = state.responseSpan(ctx);
    state.getHandler().handleReceive(null, cause, span);
    //span.flush();
    //System.out.println("TRACE HANDLER exceptionCaught: " + cause);
    ctx.fireExceptionCaught(cause);
  }


}
