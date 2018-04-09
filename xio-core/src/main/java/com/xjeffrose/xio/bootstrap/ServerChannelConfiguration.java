package com.xjeffrose.xio.bootstrap;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

public class ServerChannelConfiguration {

  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;
  private final Class<? extends ServerChannel> channelClass;

  ServerChannelConfiguration(
      EventLoopGroup bossGroup,
      EventLoopGroup workerGroup,
      Class<? extends ServerChannel> channelClass) {
    this.bossGroup = bossGroup;
    this.workerGroup = workerGroup;
    this.channelClass = channelClass;
  }

  public EventLoopGroup bossGroup() {
    return bossGroup;
  }

  public EventLoopGroup workerGroup() {
    return workerGroup;
  }

  public Class<? extends ServerChannel> channel() {
    return channelClass;
  }
}
