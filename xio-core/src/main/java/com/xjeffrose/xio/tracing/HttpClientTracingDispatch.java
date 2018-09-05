package com.xjeffrose.xio.tracing;

import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.Message;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Response;
import io.netty.channel.ChannelHandlerContext;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;

public class HttpClientTracingDispatch extends HttpTracingState {

  private final String name;
  private final Tracer tracer;

  public HttpClientTracingDispatch(String name, Tracer tracer) {
    this.name = name;
    this.tracer = tracer;
  }

  private void addRemoteIp(ChannelHandlerContext ctx, Headers headers) {
    SocketAddress address = ctx.channel().remoteAddress();
    if (address instanceof InetSocketAddress) {
      headers.set("x-remote-ip", ((InetSocketAddress) address).getHostString());
    }
  }

  public void onRequest(ChannelHandlerContext ctx, Request request) {
    addRemoteIp(ctx, request.headers());
    Optional<Span> parentSpan = request.httpTraceInfo().getSpan();
    Tracer.SpanBuilder spanBuilder =
        tracer
            .buildSpan(name + "-client")
            .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
            .withTag(Tags.HTTP_METHOD.getKey(), request.method().toString())
            .withTag(Tags.HTTP_URL.getKey(), request.path());

    parentSpan.ifPresent(spanBuilder::asChildOf);

    try (Scope scope = spanBuilder.startActive(false)) {
      tracer.inject(
          scope.span().context(),
          Format.Builtin.HTTP_HEADERS,
          new HttpHeadersInjectAdapter(request));
      request.httpTraceInfo().setSpan(scope.span());
      setSpan(ctx, scope.span());
    }
  }

  public void onResponse(ChannelHandlerContext ctx, Response response) {
    Optional<Span> requestSpan = popSpan(ctx);
    requestSpan.ifPresent(
        span -> {
          span.finish();
          response.httpTraceInfo().setSpan(span);
        });
  }

  public void onError(ChannelHandlerContext ctx, Throwable cause) {
    String message = cause.getMessage();
    if (message == null) {
      message = cause.getClass().getSimpleName();
    }
    final String value = message;
    popSpan(ctx)
        .ifPresent(
            span -> {
              span.setTag(Tags.ERROR.getKey(), value);
            });
  }

  public void onError(ChannelHandlerContext ctx, Message message, Throwable cause) {
    popSpan(ctx);
    String errorMessage = cause.getMessage();
    if (errorMessage == null) {
      errorMessage = cause.getClass().getSimpleName();
    }
    final String value = errorMessage;
    message
        .httpTraceInfo()
        .getSpan()
        .ifPresent(
            span -> {
              span.setTag(Tags.ERROR.getKey(), value);
            });
  }
}
