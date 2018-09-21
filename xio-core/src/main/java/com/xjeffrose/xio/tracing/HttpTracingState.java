package com.xjeffrose.xio.tracing;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.opentracing.Span;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HttpTracingState {

  private static final AttributeKey<Map<Integer, Span>> span_key =
      AttributeKey.newInstance("xio_tracing_span");

  public static void setSpan(ChannelHandlerContext ctx, int streamId, Span span) {
    Map<Integer, Span> spans = ctx.channel().attr(span_key).get();
    if (spans == null) {
      spans = new HashMap<>();
      ctx.channel().attr(span_key).set(spans);
    }
    spans.put(streamId, span);
  }

  public static Optional<Span> popSpan(ChannelHandlerContext ctx, int streamId) {
    Map<Integer, Span> spans = ctx.channel().attr(span_key).get();
    Span matchingSpan = spans.remove(streamId);
    return Optional.ofNullable(matchingSpan);
  }
}
