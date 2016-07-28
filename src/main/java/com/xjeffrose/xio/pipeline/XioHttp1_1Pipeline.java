package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;

public class XioHttp1_1Pipeline extends XioServerPipeline {

  private final XioPipelineFragment fragment;

  public XioHttp1_1Pipeline() {
    fragment = null;
  }

  public XioHttp1_1Pipeline(XioPipelineFragment fragment) {
    this.fragment = fragment;
  }

  public XioHttp1_1Pipeline(XioChannelHandlerFactory factory) {
    this.fragment = new XioSimplePipelineFragment(factory);
  }

  public String applicationProtocol() {
    return "http/1.1";
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
