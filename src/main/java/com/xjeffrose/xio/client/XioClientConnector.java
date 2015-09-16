package com.xjeffrose.xio.client;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public interface XioClientConnector<T extends RequestChannel> {
  ChannelFuture connect(Bootstrap bootstrap);

  T newClientChannel(Channel channel, XioClientConfig clientConfig);

  ChannelInitializer<SocketChannel> newChannelPipelineFactory(int maxFrameSize, XioClientConfig clientConfig);
}
