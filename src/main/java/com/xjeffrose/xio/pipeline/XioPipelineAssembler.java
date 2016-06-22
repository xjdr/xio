package com.xjeffrose.xio.pipeline;

import io.netty.channel.ChannelPipeline;

import java.util.List;
import java.util.ArrayList;

public class XioPipelineAssembler implements XioPipelineFragment {

  private final List<XioPipelineFragment> fragments;

  public XioPipelineAssembler() {
    fragments = new ArrayList<XioPipelineFragment>();
  }

  public void addFragment(XioPipelineFragment fragment) {
    fragments.add(fragment);
  }

  public void buildHandlers(ChannelPipeline pipeline) {
    for (XioPipelineFragment fragment : fragments) {
      fragment.buildHandlers(pipeline);
    }
  }

  public XioChannelInitializer build() {
    // TODO throw if fragments is empty
    return new XioChannelInitializer(this);
  }

}
