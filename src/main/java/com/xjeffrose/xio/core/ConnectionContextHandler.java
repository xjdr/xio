package com.xjeffrose.xio.core;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class ConnectionContextHandler extends SimpleChannelUpstreamHandler {
  @Override
  public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    super.channelConnected(ctx, e);

    XioConnectionContext context = new XioConnectionContext();
    context.setRemoteAddress(ctx.getChannel().getRemoteAddress());

    ctx.setAttachment(context);
  }
}