package com.xjeffrose.xio.pipeline;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class XioChannelInitializer extends ChannelInitializer<SocketChannel> {
  private final XioPipelineAssembler pipelineAssembler;

  public XioChannelInitializer(XioPipelineAssembler pipelineAssembler) {
    this.pipelineAssembler = pipelineAssembler;
  }

  @Override
  public void initChannel(SocketChannel channel) {
    pipelineAssembler.buildHandlers(channel.pipeline());
  }
}
