package com.xjeffrose.xio.tracing;

import brave.opentracing.BraveSpanContext;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Response;
import io.netty.channel.ChannelHandlerContext;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

class HttpServerTracingDispatch extends HttpTracingState {

  private final Tracer tracer;
  private final String name;

  public HttpServerTracingDispatch(String name, Tracer tracer) {
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
    SpanContext receivedContext =
        tracer.extract(Format.Builtin.HTTP_HEADERS, new HttpHeadersExtractAdapter(request));
    Tracer.SpanBuilder spanBuilder =
        tracer
            .buildSpan(name)
            .withTag(Tags.HTTP_METHOD.getKey(), request.method().toString())
            .withTag(Tags.HTTP_URL.getKey(), request.path());

    if (receivedContext != null) {
      if (receivedContext instanceof BraveSpanContext) {
        if (((BraveSpanContext) receivedContext).unwrap() != null) {
          spanBuilder.asChildOf(receivedContext);
        }
      } else {
        spanBuilder.asChildOf(receivedContext);
      }
    }

    try (Scope scope = spanBuilder.startActive(false)) {
      setSpan(ctx, scope.span());
      request.httpTraceInfo().setSpan(scope.span());
    }
  }

  public void onResponse(ChannelHandlerContext ctx, Response response, Throwable error) {
    popSpan(ctx)
        .ifPresent(
            span -> {
              span.finish();
              response.httpTraceInfo().setSpan(span);
            });
  }
}
