package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.propagation.TraceContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public class HttpTracingState {

  private static final AttributeKey<Span> span_key = AttributeKey.newInstance("xio_tracing_span");
  private static final AttributeKey<TraceContext> context_key = AttributeKey.newInstance("xio_tracing_context");

  public static void setSpan(ChannelHandlerContext ctx, Span span) {
    ctx.channel().attr(span_key).set(span);
  }

  public static Span getSpan(ChannelHandlerContext ctx) {
    return ctx.channel().attr(span_key).get();
  }

  public static boolean hasSpan(ChannelHandlerContext ctx) {
    return ctx.channel().attr(span_key).get() != null;
  }

  public static Span popSpan(ChannelHandlerContext ctx) {
    return ctx.channel().attr(span_key).getAndSet(null);
  }

  public static void setContext(ChannelHandlerContext ctx, TraceContext context) {
    ctx.channel().attr(context_key).set(context);
  }

  public static TraceContext popContext(ChannelHandlerContext ctx) {
    return ctx.channel().attr(context_key).getAndSet(null);
  }

}
