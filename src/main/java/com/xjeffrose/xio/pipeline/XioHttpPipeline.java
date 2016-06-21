package com.xjeffrose.xio.pipeline;

import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.channel.ChannelHandler;

import java.util.ArrayList;
import java.util.List;

public class XioHttpPipeline extends XioPipelineFragment {

  private final XioPipelineFragment fragment;

  public XioHttpPipeline() {
    fragment = null;
  }

  public XioHttpPipeline(XioPipelineFragment fragment) {
    this.fragment = fragment;
  }

  public List<ChannelHandler> buildHandlers() {
    List<ChannelHandler> result = new ArrayList<>();
    result.add(new HttpRequestDecoder());
    result.add(new HttpResponseEncoder());
    if (fragment != null) {
      result.addAll(fragment.buildHandlers());
    }
    return result;
  }

}
