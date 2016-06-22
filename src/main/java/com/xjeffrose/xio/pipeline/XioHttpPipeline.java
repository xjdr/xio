package com.xjeffrose.xio.pipeline;

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

  public void buildHandlers(ChannelPipeline pipeline) {
    pipeline.addLast(new HttpRequestDecoder());
    pipeline.addLast(new HttpResponseEncoder());
    if (fragment != null) {
      fragment.buildHandlers(pipeline);
    }
  }

}
