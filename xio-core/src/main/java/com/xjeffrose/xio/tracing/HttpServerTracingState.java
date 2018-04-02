package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.Tracer;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.TraceContext;
import com.xjeffrose.xio.http.Traceable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import lombok.Getter;

class HttpServerTracingState extends HttpTracingState {

  @Getter private final Tracer tracer;
  @Getter private final HttpServerHandler<HttpRequest, HttpResponse> handler;
  @Getter private final TraceContext.Extractor<HttpHeaders> extractor;

  public HttpServerTracingState(HttpTracing httpTracing, boolean ssl) {
    tracer = httpTracing.tracing().tracer();
    handler = HttpServerHandler.create(httpTracing, new XioHttpServerAdapter(ssl));
    extractor = httpTracing.tracing().propagation().extractor(HttpHeaders::get);
  }

  private HttpHeaders addRemoteIp(ChannelHandlerContext ctx, HttpHeaders headers) {
    SocketAddress address = ctx.channel().remoteAddress();
    if (address instanceof InetSocketAddress) {
      headers.set("x-remote-ip", ((InetSocketAddress) address).getHostString());
    }
    return headers;
  }

  public Span onRequest(ChannelHandlerContext ctx, HttpRequest request) {
    Span span = handler.handleReceive(extractor, addRemoteIp(ctx, request.headers()), request);
    setSpan(ctx, span);

    return span;
  }

  public void onResponse(ChannelHandlerContext ctx, HttpResponse response, Throwable error) {
    handler.handleSend(response, error, getSpan(ctx));
  }
}
