package com.xjeffrose.xio.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class RawBackendHandler extends ChannelInboundHandlerAdapter {

  private final Channel frontend;

  public RawBackendHandler(ChannelHandlerContext frontend) {
    this.frontend = frontend.channel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    frontend.write(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    frontend.flush();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    frontend.close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}
