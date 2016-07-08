package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelPipeline;

public interface XioPipelineFragment {

  public String applicationProtocol();

  public void buildHandlers(XioServerConfig config, XioServerState state, ChannelPipeline pipeline);

}
