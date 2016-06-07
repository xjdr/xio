package com.xjeffrose.xio.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.Logger;

public class XioL7Firewall extends ChannelDuplexHandler {
  private static final Logger log = Logger.getLogger(XioL7Firewall.class.getName());

  private final XioApplicationFirewall waf;

  public XioL7Firewall(XioApplicationFirewall waf) {
    this.waf = waf;
  }

  @Override
  @SuppressWarnings("deprecated")
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Exception Caught in L7Firewall: ", cause);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (waf.block(ctx, msg)) {
      log.info("WAF blocked :" + ctx.channel());
      ctx.channel().deregister();
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
