package com.xjeffrose.xio.pipeline;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class XioChannelInitializer extends ChannelInitializer<SocketChannel> {
  private final XioPipelineFragment pipelineFragment;

  public XioChannelInitializer(XioPipelineFragment pipelineFragment) {
    this.pipelineFragment = pipelineFragment;
  }

  @Override
  public void initChannel(SocketChannel channel) {
    pipelineFragment.buildHandlers(channel.pipeline());
  }
}
