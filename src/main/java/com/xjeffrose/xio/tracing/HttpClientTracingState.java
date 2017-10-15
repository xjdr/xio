package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpClientHandler;
import brave.http.HttpTracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import lombok.Getter;

public class HttpClientTracingState extends HttpTracingState {

  @Getter
  private final Tracing tracing;
  @Getter
  private final Tracer tracer;
  @Getter
  private final HttpClientHandler<HttpRequest, HttpResponse> handler;
  @Getter
  private final TraceContext.Injector<HttpHeaders> injector;

  public HttpClientTracingState(HttpTracing httpTracing, boolean ssl) {
    tracing = httpTracing.tracing();
    tracer = tracing.tracer();
    handler = HttpClientHandler.create(httpTracing, new XioHttpClientAdapter(ssl));
    injector = httpTracing.tracing().propagation().injector(HttpHeaders::set);
  }

  private HttpHeaders addRemoteIp(ChannelHandlerContext ctx, HttpHeaders headers) {
    SocketAddress address = ctx.channel().remoteAddress();
    if (address instanceof InetSocketAddress) {
      headers.set("x-remote-ip", ((InetSocketAddress)address).getHostString());
    }
    return headers;
  }

  public Span onRequest(ChannelHandlerContext ctx, HttpRequest request) {
    TraceContext parent = popContext(ctx);
    CurrentTraceContext.Scope scope = null;
    if (parent != null) {
      scope = tracing.currentTraceContext().newScope(parent);
    }
    Span span = handler.handleSend(injector, addRemoteIp(ctx, request.headers()), request);
    setSpan(ctx, span);

    if (scope != null) {
      scope.close();
    }

    return span;
  }

  public void onResponse(ChannelHandlerContext ctx, HttpResponse response) {
    Span span = popSpan(ctx);
    if (span != null) {
      handler.handleReceive(response, null, span);
    }
  }

  public void onError(ChannelHandlerContext ctx, Throwable cause) {
    Span span = popSpan(ctx);
    if (span != null) {
      handler.handleReceive(null, cause, span);
    }
  }

}
