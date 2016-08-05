package com.xjeffrose.xio.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Utility class used to build ClientChannelConfiguration and ServerChannelConfiguration.
 */
public class ChannelConfiguration {

  /**
   * This method will configure a worker EventLoopGroup and a Channel
   * for use by a client. It will try to use Epoll if it's available.
   */
  static public ClientChannelConfiguration clientConfig(int workerThreads) {
    EventLoopGroup workerGroup;
    Class<? extends Channel> channelClass;
    if (Epoll.isAvailable()) {
      workerGroup = new EpollEventLoopGroup(workerThreads);
      channelClass = EpollSocketChannel.class;
    } else {
      workerGroup = new NioEventLoopGroup(workerThreads);
      channelClass = NioSocketChannel.class;
    }

    return new ClientChannelConfiguration(workerGroup, channelClass);
  }

  /**
   * This method will configure a worker EventLoopGroup and a Channel
   * for use by a client. It will try to use the correct SocketChannel
   * for the provided workerGroup.
   */
  static public ClientChannelConfiguration clientConfig(EventLoopGroup workerGroup) {
    EventLoopGroup parent = workerGroup;
    if (parent instanceof EventLoop) {
      parent = ((EventLoop)workerGroup).parent();
    }
    Class<? extends Channel> channelClass;
    if (parent instanceof EpollEventLoopGroup) {
      channelClass = EpollSocketChannel.class;
    } else if (parent instanceof NioEventLoopGroup) {
      channelClass = NioSocketChannel.class;
    } else {
      throw new RuntimeException("Unsupported EventLoopGroup " + workerGroup.getClass());
    }

    return new ClientChannelConfiguration(workerGroup, channelClass);
  }


  /**
   * This method will configure a boss EventLoopGroup, a worker
   * EventLoopGroup and a ServerChannel for use by a server. It will
   * try to use Epoll if it's available.
   */
  static public ServerChannelConfiguration serverConfig(int bossThreads, int workerThreads) {
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

    return new ServerChannelConfiguration(bossGroup, workerGroup, channelClass);
  }
}
