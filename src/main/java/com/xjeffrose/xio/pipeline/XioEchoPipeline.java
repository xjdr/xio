package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.EchoCodec;
import io.netty.channel.ChannelHandler;

public class XioEchoPipeline extends XioServerPipeline {

  public XioEchoPipeline() {
  }

  public String applicationProtocol() {
    return "echo";
  }

  public ChannelHandler getCodecHandler() {
    return new EchoCodec();
  }

}
