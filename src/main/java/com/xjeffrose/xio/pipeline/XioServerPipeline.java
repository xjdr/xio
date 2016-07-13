package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.server.XioServerLimits;
import io.netty.channel.ChannelHandler;

public class XioServerPipeline extends XioBasePipeline {

  public XioServerPipeline() {
  }

  public ChannelHandler getEncryptionHandler() {
    return null;
  }

  public ChannelHandler getAuthenticationHandler() {
    return null;
  }

  public ChannelHandler getCodecHandler() {
    return null;
  }

  public ChannelHandler getIdleDisconnectHandler(XioServerLimits limits) {
    return new XioIdleDisconnectHandler(
      limits.maxReadIdleTime(),
      limits.maxWriteIdleTime(),
      limits.maxAllIdleTime()
    );
  }

  public String applicationProtocol() {
    return null;
  }
}
