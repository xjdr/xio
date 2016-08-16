package com.xjeffrose.xio.server.trailhead;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class RawBackendHandler extends ChannelInboundHandlerAdapter {

  private final ChannelHandlerContext frontend;

  public RawBackendHandler(ChannelHandlerContext frontend) {
    this.frontend = frontend;
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
