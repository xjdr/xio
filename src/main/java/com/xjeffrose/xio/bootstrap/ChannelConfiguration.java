package com.xjeffrose.xio.bootstrap;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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

import java.util.concurrent.ThreadFactory;

/**
 * Utility class used to build ClientChannelConfiguration and ServerChannelConfiguration.
 */
public class ChannelConfiguration {

  static private ThreadFactory threadFactory(String nameFormat) {
    return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
  }

  /**
   * This method will configure a worker EventLoopGroup and a Channel
   * for use by a client. It will try to use Epoll if it's available.
   *
   * @param workerThreads the number of worker {@link Thread}s to allocate
   * @param workerNameFormat the name format passed to {@link ThreadFactoryBuilder#setNameFormat(String)}
   * @return a {@link ClientChannelConfiguration}
   */
  static public ClientChannelConfiguration clientConfig(int workerThreads, String workerNameFormat) {
    EventLoopGroup workerGroup;
    Class<? extends Channel> channelClass;
    if (Epoll.isAvailable()) {
      workerGroup = new EpollEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
      channelClass = EpollSocketChannel.class;
    } else {
      workerGroup = new NioEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
      channelClass = NioSocketChannel.class;
    }

    return new ClientChannelConfiguration(workerGroup, channelClass);
  }

  /**
   * This method will configure a worker EventLoopGroup and a Channel
   * for use by a client. It will try to use the correct SocketChannel
   * for the provided workerGroup.
   *
   * @param workerGroup an existing {@link EventLoopGroup}
   * @return a {@link ClientChannelConfiguration}
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
   *
   * @param bossThreads the number of {@link Thread}s to allocate for the bossGroup
   * @param bossNameFormat the name format passed to {@link ThreadFactoryBuilder#setNameFormat(String)}
   * @param workerThreads the number of worker {@link Thread}s to allocate for the workerGroup
   * @param workerNameFormat the name format passed to {@link ThreadFactoryBuilder#setNameFormat(String)}
   * @return a {@link ServerChannelConfiguration}
   */
  static public ServerChannelConfiguration serverConfig(int bossThreads, String bossNameFormat, int workerThreads, String workerNameFormat) {
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;
    Class<? extends ServerChannel> channelClass;
    if (Epoll.isAvailable()) {
      bossGroup = new EpollEventLoopGroup(bossThreads, threadFactory(bossNameFormat));
      workerGroup = new EpollEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
      channelClass = EpollServerSocketChannel.class;
    } else {
      bossGroup = new NioEventLoopGroup(bossThreads, threadFactory(bossNameFormat));
      workerGroup = new NioEventLoopGroup(workerThreads, threadFactory(workerNameFormat));
      channelClass = NioServerSocketChannel.class;
    }

    return new ServerChannelConfiguration(bossGroup, workerGroup, channelClass);
  }
}
