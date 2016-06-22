package com.xjeffrose.xio.pipeline;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import java.util.List;

public class XioChannelInitializer extends ChannelInitializer<SocketChannel> {
  private final XioPipelineAssembler pipelineAssembler;

  public XioChannelInitializer(XioPipelineAssembler pipelineAssembler) {
    this.pipelineAssembler = pipelineAssembler;
  }

  @Override
  public void initChannel(SocketChannel channel) {
    for (ChannelHandler handler : pipelineAssembler.buildHandlers()) {
      channel.pipeline().addLast(handler);
    }
  }
}
