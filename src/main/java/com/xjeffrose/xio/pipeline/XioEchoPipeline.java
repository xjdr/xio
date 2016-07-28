package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.EchoCodec;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;

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
