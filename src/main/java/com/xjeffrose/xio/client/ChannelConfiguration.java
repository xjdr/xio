package com.xjeffrose.xio.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.Channel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * This class will configure an EventLoopGroup and a Channel for use
 * by a client. It will try to use Epoll if it's available.
 */
public class ChannelConfiguration {

  private final EventLoopGroup workerGroup;
  private final Class<? extends Channel> channelClass;

  private ChannelConfiguration(EventLoopGroup workerGroup, Class<? extends Channel> channelClass) {
    this.workerGroup = workerGroup;
    this.channelClass = channelClass;
  }

  public EventLoopGroup workerGroup() {
    return workerGroup;
  }

  public Class<? extends Channel> channel() {
    return channelClass;
  }

  static public ChannelConfiguration clientConfig(int workerThreads) {
    EventLoopGroup workerGroup;
    Class<? extends Channel> channelClass;
    if (Epoll.isAvailable()) {
      workerGroup = new EpollEventLoopGroup(workerThreads);
      channelClass = EpollSocketChannel.class;
    } else {
      workerGroup = new NioEventLoopGroup(workerThreads);
      channelClass = NioSocketChannel.class;
    }

    return new ChannelConfiguration(workerGroup, channelClass);
  }

  static public ChannelConfiguration clientConfig(EventLoopGroup workerGroup) {
    Class<? extends Channel> channelClass;
    if (workerGroup instanceof EpollEventLoopGroup) {
      channelClass = EpollSocketChannel.class;
    } else if (workerGroup instanceof NioEventLoopGroup) {
      channelClass = NioSocketChannel.class;
    } else {
      throw new RuntimeException("Unsupported EventLoopGroup");
    }

    return new ChannelConfiguration(workerGroup, channelClass);
  }
}
