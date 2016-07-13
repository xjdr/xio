package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelPipeline;

public class XioSimplePipelineFragment implements XioPipelineFragment {

  private final String pipelineLabel;
  private final XioChannelHandlerFactory factory;

  public XioSimplePipelineFragment(String pipelineLabel, XioChannelHandlerFactory factory) {
    this.pipelineLabel = pipelineLabel;
    this.factory = factory;
  }
  public XioSimplePipelineFragment(XioChannelHandlerFactory factory) {
    this("XioSimplePipelineFragment", factory);
  }

  public String applicationProtocol() {
    return null;
  }

  public void buildHandlers(XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
    pipeline.addLast(pipelineLabel, factory.build());
  }
}
