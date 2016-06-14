package com.xjeffrose.xio.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import java.util.HashSet;
import org.apache.log4j.Logger;

public abstract class XioFirewall extends ChannelDuplexHandler {
  private static final Logger log = Logger.getLogger(XioFirewall.class.getName());

  private final HashSet<String> blacklist;
  private final HashSet<String> whitelist;
  private boolean noOp;
  private int packetSize;
  private int destinationPort;
  private String destinationAddress;
  private int sourcePort;
  private String sourceAddress;

  public XioFirewall(boolean noOp) {
    this.blacklist = null;
    this.whitelist = null;
    this.noOp = noOp;
  }

  public XioFirewall(HashSet blacklist, HashSet whitelist) {
    this.blacklist = blacklist;
    this.whitelist = whitelist;
    this.noOp = false;
  }

  private void buildReqCtx(ChannelHandlerContext ctx) {
    destinationPort = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
    destinationAddress = ((InetSocketAddress) ctx.channel().localAddress()).getHostString();
    sourcePort = ((InetSocketAddress) ctx.channel().remoteAddress()).getPort();
    sourceAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();
  }

  @Override
  @SuppressWarnings("deprecated")
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Exception Caught in Xio Firewall: ", cause);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    if (noOp) {
      ctx.pipeline().remove(this);
      ctx.fireChannelActive();
    }

    buildReqCtx(ctx);

    if (!whitelist.contains(sourceAddress)) {
      if (blacklist.contains(sourceAddress)) {
        log.info("Xio Firewall blocked blacklisted channel:" + ctx.channel());
        ctx.channel().deregister();
      } else {
        ctx.fireChannelActive();
      }
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf) {
      packetSize = ((ByteBuf) msg).readableBytes();
    } else {
      log.info("Xio Firewall blocked unreadable message :" + ctx.channel());
      ctx.channel().deregister();
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
