package com.xjeffrose.xio.bootstrap;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * This class will configure two EventLoopGroups and a Channel for use
 * by a server. It will try to use Epoll if it's available.
 */
public class ChannelConfiguration {

  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;
  private final Class<? extends ServerChannel> channelClass;

  private ChannelConfiguration(EventLoopGroup bossGroup, EventLoopGroup workerGroup, Class<? extends ServerChannel> channelClass) {
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

  static public ChannelConfiguration serverConfig(int bossThreads, int workerThreads) {
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    Class<? extends ServerChannel> channelClass;
    if (Epoll.isAvailable()) {
      bossGroup = new EpollEventLoopGroup(bossThreads);
      workerGroup = new EpollEventLoopGroup(workerThreads);
      channelClass = EpollServerSocketChannel.class;
    } else {
      bossGroup = new NioEventLoopGroup(bossThreads);
      workerGroup = new NioEventLoopGroup(workerThreads);
      channelClass = NioServerSocketChannel.class;
    }

    return new ChannelConfiguration(bossGroup, workerGroup, channelClass);
  }
}
