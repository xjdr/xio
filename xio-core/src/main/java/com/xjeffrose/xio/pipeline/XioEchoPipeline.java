package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.EchoCodec;
import com.xjeffrose.xio.server.XioServerConfig;
import io.netty.channel.ChannelHandler;

public class XioEchoPipeline extends XioServerPipeline {

  public XioEchoPipeline() {}

  @Override
  public String applicationProtocol() {
    return "echo";
  }

  @Override
  public ChannelHandler getCodecHandler(XioServerConfig config) {
    return new EchoCodec();
  }
}
