package com.xjeffrose.xio.pipeline;

import io.netty.channel.ChannelHandler;

import java.util.List;
import java.util.ArrayList;

public class XioPipelineAssembler {

  private final List<XioPipelineFragment> fragments;

  public XioPipelineAssembler() {
    fragments = new ArrayList<XioPipelineFragment>();
  }

  public void addFragment(XioPipelineFragment fragment) {
    fragments.add(fragment);
  }

  public List<ChannelHandler> buildHandlers() {
    ArrayList<ChannelHandler> result = new ArrayList<ChannelHandler>();
    for (XioPipelineFragment fragment : fragments) {
      result.addAll(fragment.buildHandlers());
    }
    return result;
  }

  public XioChannelInitializer build() {
    // TODO throw if fragments is empty
    return new XioChannelInitializer(this);
  }

}
