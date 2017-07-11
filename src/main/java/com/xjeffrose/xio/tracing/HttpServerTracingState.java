package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;
import lombok.Getter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

class HttpServerTracingState {

  private static final AttributeKey<Tracer> tracer_key = AttributeKey.newInstance("xio_server_tracing_tracer");
  private static final AttributeKey<Span> span_key = AttributeKey.newInstance("xio_server_tracing_span");
  private static final AttributeKey<Tracer.SpanInScope> span_in_scope_key = AttributeKey.newInstance("xio_server_tracing_span_in_scope");

  @Getter
  private final Tracer tracer;
  @Getter
  private final HttpServerHandler<HttpRequest, HttpResponse> handler;
  @Getter
  private final TraceContext.Extractor<HttpHeaders> extractor;

  public HttpServerTracingState(HttpTracing httpTracing, boolean ssl) {
    tracer = httpTracing.tracing().tracer();
    handler = HttpServerHandler.create(httpTracing, new XioHttpServerAdapter(ssl));
    extractor = httpTracing.tracing().propagation().extractor(HttpHeaders::get);
  }

  private HttpHeaders addRemoteIp(ChannelHandlerContext ctx, HttpHeaders headers) {
    SocketAddress address = ctx.channel().remoteAddress();
    if (address instanceof InetSocketAddress) {
      headers.set("x-remote-ip", ((InetSocketAddress)address).getHostString());
    }
    return headers;
  }

  public static void attachTracer(Channel ch, Tracer tracer) {
    ch.attr(tracer_key).set(tracer);
  }

  public static void attachSpan(Channel ch, Span span) {
    ch.attr(span_key).set(span);
  }

  public static void attachSpanInScope(Channel ch, Tracer.SpanInScope spanInScope) {
    ch.attr(span_in_scope_key).set(spanInScope);
  }

  public Span requestSpan(ChannelHandlerContext ctx, HttpRequest request) {
    Span span = handler.handleReceive(extractor, addRemoteIp(ctx, request.headers()), request);
    attachSpan(ctx.channel(), span);
    attachTracer(ctx.channel(), tracer);

    return span;
  }

  public Tracer.SpanInScope requestSpanInScope(ChannelHandlerContext ctx, Span span) {
    Tracer.SpanInScope spanInScope = tracer.withSpanInScope(span);
    attachSpanInScope(ctx.channel(), spanInScope);

    return spanInScope;
  }

  public static Tracer tracer(Channel ch) {
    return ch.attr(tracer_key).get();
  }

  public static Tracer tracer(ChannelHandlerContext ctx) {
    return tracer(ctx.channel());
  }

  public static Span responseSpan(Channel ch) {
    return ch.attr(span_key).get();
  }

  public static Span responseSpan(ChannelHandlerContext ctx) {
    return responseSpan(ctx.channel());
  }

  public static Tracer.SpanInScope responseSpanInScope(Channel ch) {
    return ch.attr(span_in_scope_key).get();
  }

  public static Tracer.SpanInScope responseSpanInScope(ChannelHandlerContext ctx) {
    return responseSpanInScope(ctx.channel());
  }
}
