package com.xjeffrose.xio.core;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

@ChannelHandler.Sharable
public class XioNoOpHandler extends ChannelDuplexHandler {
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.pipeline().remove(this);
    super.channelActive(ctx);
  }
}
