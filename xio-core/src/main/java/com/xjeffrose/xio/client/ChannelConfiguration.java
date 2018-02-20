package com.xjeffrose.xio.client;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * This class will configure an EventLoopGroup and a Channel for use by a client. It will try to use
 * Epoll if it's available.
 */
// TODO(CK): this needs to move into the bootstrap package
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

  public static ChannelConfiguration clientConfig(int workerThreads) {
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

  public static ChannelConfiguration clientConfig(EventLoopGroup workerGroup) {
    EventLoopGroup parent = workerGroup;
    if (parent instanceof EventLoop) {
      parent = ((EventLoop) workerGroup).parent();
    }
    Class<? extends Channel> channelClass;
    if (parent instanceof EpollEventLoopGroup) {
      channelClass = EpollSocketChannel.class;
    } else if (parent instanceof NioEventLoopGroup) {
      channelClass = NioSocketChannel.class;
    } else {
      throw new RuntimeException("Unsupported EventLoopGroup " + workerGroup.getClass());
    }

    return new ChannelConfiguration(workerGroup, channelClass);
  }
}
