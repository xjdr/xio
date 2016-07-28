package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class XioHttpPipeline implements XioPipelineFragment {

  private final XioPipelineFragment fragment;

  public XioHttpPipeline() {
    fragment = null;
  }

  public XioHttpPipeline(XioPipelineFragment fragment) {
    this.fragment = fragment;
  }

  public String applicationProtocol() {
    return "http/1.1";
  }

  public void buildHandlers(XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
    pipeline.addLast(new HttpRequestDecoder());
    pipeline.addLast(new HttpResponseEncoder());
    if (fragment != null) {
      fragment.buildHandlers(config, state, pipeline);
    }
  }

}
