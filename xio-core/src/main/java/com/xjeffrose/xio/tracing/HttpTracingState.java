package com.xjeffrose.xio.tracing;

import brave.Span;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import java.util.Optional;

public class HttpTracingState {

  private static final AttributeKey<Span> span_key = AttributeKey.newInstance("xio_tracing_span");

  public static void setSpan(ChannelHandlerContext ctx, Span span) {
    ctx.channel().attr(span_key).set(span);
  }

  public static Optional<Span> popSpan(ChannelHandlerContext ctx) {
    return Optional.ofNullable(ctx.channel().attr(span_key).getAndSet(null));
  }
}
