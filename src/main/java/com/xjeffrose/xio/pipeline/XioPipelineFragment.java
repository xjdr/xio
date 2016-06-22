package com.xjeffrose.xio.pipeline;

import io.netty.channel.ChannelPipeline;

public interface XioPipelineFragment {

  public void buildHandlers(ChannelPipeline pipeline);

}
