package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerInstrumentation;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelPipeline;

import java.util.List;
import java.util.ArrayList;

public class XioPipelineAssembler {

  private final XioServerConfig config;
  private final List<XioPipelineFragment> fragments;

  public XioPipelineAssembler(XioServerConfig config) {
    this.config = config;
    fragments = new ArrayList<XioPipelineFragment>();
  }

  public void addFragment(XioPipelineFragment fragment) {
    fragments.add(fragment);
  }

  public void buildHandlers(ChannelPipeline pipeline) {
    for (XioPipelineFragment fragment : fragments) {
      XioServerState state = null;

      fragment.buildHandlers(config, state, pipeline);
    }
  }

  private XioChannelInitializer buildInitializer() {
    if (fragments.size() == 0) {
      throw new RuntimeException("At least one pipeline fragment needs to be added prior to calling build");
    }

    return new XioChannelInitializer(this);
  }

  public XioChannelInitializer build(XioServerInstrumentation instrumentation) {
    for (XioPipelineFragment fragment : fragments) {
      String protocol = fragment.applicationProtocol();
      if (protocol != null) {
        if (instrumentation.applicationProtocol != null) {
          throw new RuntimeException("Only one application protocol can be defined for a given pipeline");
        }
        instrumentation.applicationProtocol = protocol;
      }
    }

    return buildInitializer();
  }

}
