package com.xjeffrose.xio.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@ChannelHandler.Sharable
@Slf4j
public class XioConnectionLimiter extends ChannelDuplexHandler {


  private final AtomicInteger numConnections;
  private final int maxConnections;

  public XioConnectionLimiter(int maxConnections) {
    this.maxConnections = maxConnections;
    this.numConnections = new AtomicInteger(0);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    if (maxConnections > 0) {
      if (numConnections.incrementAndGet() > maxConnections) {
        ctx.channel().close();
        // numConnections will be decremented in channelClosed
        log.info("Accepted connection above limit (" + maxConnections + "). Dropping.");
      }
    }
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (maxConnections > 0) {
      if (numConnections.decrementAndGet() < 0) {
        log.error("BUG in ConnectionLimiter");
      }
    }
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
