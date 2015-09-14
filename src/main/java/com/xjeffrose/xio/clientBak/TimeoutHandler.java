package com.xjeffrose.xio.clientBak;


import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import static com.google.common.base.Preconditions.checkNotNull;

public final class TimeoutHandler extends ChannelDuplexHandler {
  private static final String NAME = "_TIMEOUT_HANDLER";

  private volatile long lastMessageReceivedNanos = 0L;
  private volatile long lastMessageSentNanos = 0L;

  private TimeoutHandler() {
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
    lastMessageReceivedNanos = System.nanoTime();
    ctx.fireChannelRead(o);
  }

  public static synchronized void addToPipeline(ChannelPipeline cp) {
    checkNotNull(cp, "cp is null");
    if (cp.get(NAME) == null) {
      cp.addFirst(NAME, new TimeoutHandler());
    }
  }

  public static TimeoutHandler findTimeoutHandler(ChannelPipeline cp) {
    return (TimeoutHandler) cp.get(NAME);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    lastMessageSentNanos = System.nanoTime();
    ctx.fireChannelWritabilityChanged();
  }

  public long getLastMessageReceivedNanos() {
    return lastMessageReceivedNanos;
  }

  public long getLastMessageSentNanos() {
    return lastMessageSentNanos;
  }
}