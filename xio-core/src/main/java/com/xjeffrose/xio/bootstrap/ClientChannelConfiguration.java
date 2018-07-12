package com.xjeffrose.xio.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

public class ClientChannelConfiguration {

  private final EventLoopGroup workerGroup;
  private final Class<? extends Channel> channelClass;

  public ClientChannelConfiguration(
      EventLoopGroup workerGroup, Class<? extends Channel> channelClass) {
    this.workerGroup = workerGroup;
    this.channelClass = channelClass;
  }

  public EventLoopGroup workerGroup() {
    return workerGroup;
  }

  public Class<? extends Channel> channel() {
    return channelClass;
  }
}
