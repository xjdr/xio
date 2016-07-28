package com.xjeffrose.xio.server;

import io.netty.channel.ChannelHandlerContext;

interface XioApplicationFirewall {

  boolean block(ChannelHandlerContext ctx, Object msg);

}
