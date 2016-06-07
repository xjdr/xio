package com.xjeffrose.xio.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import java.util.HashSet;
import org.apache.log4j.Logger;

public class XioL4Firewall extends ChannelDuplexHandler {
  private static final Logger log = Logger.getLogger(XioL4Firewall.class.getName());

  private final HashSet<String> blacklist;

  public XioL4Firewall(HashSet blacklist) {
    this.blacklist = blacklist;
  }

  @Override
  @SuppressWarnings("deprecated")
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Exception Caught in L4Firewall: ", cause);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    if (blacklist.contains(((InetSocketAddress) ctx.channel().remoteAddress()).getHostString())) {
      log.info("L4 Firewall blocked :" + ctx.channel());
      ctx.channel().deregister();
    } else {
      ctx.fireChannelActive();
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
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
