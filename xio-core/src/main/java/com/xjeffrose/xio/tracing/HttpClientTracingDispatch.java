package com.xjeffrose.xio.tracing;

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
import java.util.stream.StreamSupport;

public class HttpClientTracingDispatch extends HttpTracingState {

  private final String name;
  private final Tracer tracer;

  public HttpClientTracingDispatch(String name, Tracer tracer) {
    this.name = name;
    this.tracer = tracer;
  }

  private String remoteIp(ChannelHandlerContext ctx) {
    SocketAddress address = ctx.channel().remoteAddress();
    if (address instanceof InetSocketAddress) {
      return ((InetSocketAddress) address).getHostString();
    }

    return "unknown";
  }

  private String localIp(ChannelHandlerContext ctx) {
    SocketAddress address = ctx.channel().localAddress();
    if (address instanceof InetSocketAddress) {
      return ((InetSocketAddress) address).getAddress().getHostAddress();
    }

    return "unknown";
  }

  public void onRequest(ChannelHandlerContext ctx, Request request) {
    String requestorIpAddress = remoteIp(ctx);
    if (request.headers().get("x-remote-ip") != null) {
      requestorIpAddress = request.headers().get("x-remote-ip");
    }
    request.headers().set("x-remote-ip", requestorIpAddress);

    String httpType = request.streamId() == Message.H1_STREAM_ID_NONE ? "http1.1" : "h2";
    Tracer.SpanBuilder spanBuilder =
        tracer
            .buildSpan(name + ".client")
            .withTag(Tags.HTTP_METHOD.getKey(), request.method().toString())
            .withTag(Tags.HTTP_URL.getKey(), request.path())
            .withTag("http.request.type", httpType)
            .withTag("http.request.streamId", request.streamId())
            .withTag("http.request.source-ip-address", localIp(ctx))
            .withTag("http.request.originating-ip-address", requestorIpAddress);

    StreamSupport.stream(request.headers().spliterator(), false)
        .forEach(
            (entry) -> {
              spanBuilder.withTag(
                  "http.request.headers." + entry.getKey().toString(), entry.getValue().toString());
            });

    Optional<Span> parentSpan = request.httpTraceInfo().getSpan();
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
          int responseCode = response.status().code();
          span.setTag(Tags.HTTP_STATUS.getKey(), responseCode);
          span.setTag("http.response.streamId", response.streamId());
          StreamSupport.stream(response.headers().spliterator(), false)
              .forEach(
                  (entry) -> {
                    span.setTag(
                        "http.response.headers." + entry.getKey().toString(),
                        entry.getValue().toString());
                  });
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
