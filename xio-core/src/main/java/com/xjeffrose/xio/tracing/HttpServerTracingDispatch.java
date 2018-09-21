package com.xjeffrose.xio.tracing;

import brave.opentracing.BraveSpanContext;
import com.xjeffrose.xio.http.Message;
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
import java.util.stream.StreamSupport;

class HttpServerTracingDispatch extends HttpTracingState {

  private final Tracer tracer;
  private final String name;

  public HttpServerTracingDispatch(String name, Tracer tracer) {
    this.name = name;
    this.tracer = tracer;
  }

  private String remoteIp(ChannelHandlerContext ctx) {
    SocketAddress address = ctx.channel().remoteAddress();
    if (address instanceof InetSocketAddress) {
      return ((InetSocketAddress) address).getHostString();
    }

    return null;
  }

  private String localIp(ChannelHandlerContext ctx) {
    SocketAddress address = ctx.channel().localAddress();
    if (address instanceof InetSocketAddress) {
      return ((InetSocketAddress) address).getAddress().getHostAddress();
    }

    return null;
  }

  public void onRequest(ChannelHandlerContext ctx, Request request) {
    String requestorIpAddress = remoteIp(ctx);
    if (request.headers().get("x-remote-ip") != null) {
      requestorIpAddress = request.headers().get("x-remote-ip");
    }
    requestorIpAddress = requestorIpAddress != null ? requestorIpAddress : "unknown";
    request.headers().set("x-remote-ip", requestorIpAddress);

    String httpType = request.streamId() == Message.H1_STREAM_ID_NONE ? "http1.1" : "h2";
    Tracer.SpanBuilder spanBuilder =
        tracer
            .buildSpan(name)
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

    SpanContext receivedContext =
        tracer.extract(Format.Builtin.HTTP_HEADERS, new HttpHeadersExtractAdapter(request));
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
}
