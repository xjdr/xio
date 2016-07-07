package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.server.XioSecurityHandlers;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerLimits;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

import java.util.concurrent.TimeUnit;

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
    return "";
  }
}
