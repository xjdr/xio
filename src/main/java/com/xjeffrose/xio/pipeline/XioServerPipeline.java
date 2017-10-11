package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerLimits;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelHandler;

// TODO(CK): merge this with XioBasePipeline
public class XioServerPipeline extends XioBasePipeline {

  public XioServerPipeline() {
  }

  @Override
  public ChannelHandler getEncryptionHandler(XioServerConfig config, XioServerState state) {
    return null;
  }

  @Override
  public ChannelHandler getAuthenticationHandler() {
    return null;
  }

  @Override
  public ChannelHandler getCodecNegotiationHandler(XioServerConfig config) {
    return null;
  }

  @Override
  public ChannelHandler getCodecHandler(XioServerConfig config) {
    return null;
  }

  @Override
  public ChannelHandler getIdleDisconnectHandler(XioServerLimits limits) {
    return new XioIdleDisconnectHandler(
      limits.maxReadIdleTime(),
      limits.maxWriteIdleTime(),
      limits.maxAllIdleTime()
    );
  }

  @Override
  public String applicationProtocol() {
    return null;
  }

  @Override
  public ChannelHandler getApplicationHandler() {
    return null;
  }

}
