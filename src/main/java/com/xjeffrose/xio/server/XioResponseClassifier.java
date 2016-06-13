package com.xjeffrose.xio.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Logger;

public class XioResponseClassifier extends ChannelDuplexHandler {
  private static final Logger log = Logger.getLogger(XioResponseClassifier.class.getName());
  private boolean noOp;

  public XioResponseClassifier(boolean noOp) {
    this.noOp = noOp;
  }


  @Override
  @SuppressWarnings("deprecated")
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("Exception Caught in XioResponseClassifier: ", cause);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    if (noOp) {
      ctx.pipeline().remove(this);
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
