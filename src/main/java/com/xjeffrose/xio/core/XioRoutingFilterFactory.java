package com.xjeffrose.xio.core;

import io.netty.channel.ChannelInboundHandler;

public interface XioRoutingFilterFactory {
  ChannelInboundHandler getRoutingFilter();
}
