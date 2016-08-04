package com.xjeffrose.xio.bootstrap;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.Channel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ClientChannelConfiguration {

  private final EventLoopGroup workerGroup;
  private final Class<? extends Channel> channelClass;

  ClientChannelConfiguration(EventLoopGroup workerGroup, Class<? extends Channel> channelClass) {
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
