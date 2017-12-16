package com.xjeffrose.xio.pipeline;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

public final class Pipelines {

  public static ChannelPipeline addHandler(
      ChannelPipeline pipeline, String name, ChannelHandler handler) {
    if (handler != null) {
      pipeline.addLast(name, handler);
    }
    return pipeline;
  }
}
