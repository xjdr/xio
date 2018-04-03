package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.Tracer;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.TraceContext;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Response;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import lombok.Getter;

class HttpServerTracingDispatch {

  @Getter private final Tracer tracer;
  @Getter private final HttpServerHandler<Request, Response> handler;
  @Getter private final TraceContext.Extractor<Headers> extractor;

  public HttpServerTracingDispatch(HttpTracing httpTracing, boolean ssl) {
    tracer = httpTracing.tracing().tracer();
    handler = HttpServerHandler.create(httpTracing, new XioHttpServerAdapter(ssl));
    extractor = httpTracing.tracing().propagation().extractor(Headers::get);
  }

  private Headers addRemoteIp(ChannelHandlerContext ctx, Headers headers) {
    SocketAddress address = ctx.channel().remoteAddress();
    if (address instanceof InetSocketAddress) {
      headers.set("x-remote-ip", ((InetSocketAddress) address).getHostString());
    }
    return headers;
  }

  public void onRequest(ChannelHandlerContext ctx, Request request) {
    Span span = handler.handleReceive(extractor, addRemoteIp(ctx, request.headers()), request);
    request.httpTraceInfo().setSpan(span);
  }

  public void onResponse(Response response, Throwable error) {
    response.httpTraceInfo().getSpan().ifPresent(span -> handler.handleSend(response, error, span));
  }
}
