package com.xjeffrose.xio.client;


import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipelineFactory;

public interface XioClientConnector<T extends RequestChannel> {
  ChannelFuture connect(ClientBootstrap bootstrap);

  T newClientChannel(Channel channel, XioClientConfig clientConfig);

  ChannelPipelineFactory newChannelPipelineFactory(int maxFrameSize, XioClientConfig clientConfig);
}
