package com.xjeffrose.xio.core;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;


public class ConnectionContextHandler extends ChannelDuplexHandler {
  private static final AttributeKey<XioConnectionContext> CONNECTION_CONTEXT = AttributeKey.valueOf("XioConnectionContext");

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);

    XioConnectionContext context = new XioConnectionContext();
    context.setRemoteAddress(ctx.channel().remoteAddress());

    ctx.attr(CONNECTION_CONTEXT).set(context);
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
