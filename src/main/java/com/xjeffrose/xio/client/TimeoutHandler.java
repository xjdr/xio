package com.xjeffrose.xio.client;


import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;

import static com.google.common.base.Preconditions.checkNotNull;

public final class TimeoutHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {
  private static final String NAME = "_TIMEOUT_HANDLER";

  private volatile long lastMessageReceivedNanos = 0L;
  private volatile long lastMessageSentNanos = 0L;

  private TimeoutHandler() {
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
  public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
      throws Exception {
    if (e instanceof MessageEvent) {
      lastMessageReceivedNanos = System.nanoTime();
    }
    ctx.sendUpstream(e);
  }

  @Override
  public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
      throws Exception {
    if (e instanceof MessageEvent) {
      lastMessageSentNanos = System.nanoTime();
    }
    ctx.sendDownstream(e);
  }

  public long getLastMessageReceivedNanos() {
    return lastMessageReceivedNanos;
  }

  public long getLastMessageSentNanos() {
    return lastMessageSentNanos;
  }
}