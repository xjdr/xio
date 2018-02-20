package com.xjeffrose.xio.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TcpAggregator extends ChannelInboundHandlerAdapter {

  CompositeByteBuf byteBufs;

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    byteBufs = ctx.alloc().compositeBuffer();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, Object msg) {
    byteBufs.addComponent((ByteBuf) msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    ctx.fireChannelRead(byteBufs);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    closeOnFlush(ctx.channel());
  }

  static void closeOnFlush(Channel ch) {
    if (ch.isActive()) {
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }
}
