package com.xjeffrose.xio.core;

import io.netty.channel.ChannelHandler;

public interface XioAggregatorFactory {

  ChannelHandler getAggregator();

}
