package com.xjeffrose.xio.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

public class IdleDisconnectHandler extends IdleStateHandler {
  public IdleDisconnectHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
    super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
  }

  @Override
  protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
    ctx.channel().close();
  }

//    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
//    ctx.getChannel().close();
//  }
}