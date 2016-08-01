package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * TODO(CK): flesh out http/2 functionality
 */
public class XioHttp2Pipeline extends XioServerPipeline {

  private final XioPipelineFragment fragment;

  public XioHttp2Pipeline() {
    fragment = null;
  }

  public XioHttp2Pipeline(XioPipelineFragment fragment) {
    this.fragment = fragment;
  }

  public String applicationProtocol() {
    return "http/2";
  }

  public ChannelHandler getCodecHandler() {
    return new HttpServerCodec();
  }

  public void buildHandlers(XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
    super.buildHandlers(config, state, pipeline);
    if (fragment != null) {
      fragment.buildHandlers(config, state, pipeline);
    }
  }

}
