package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.server.XioSecurityHandlers;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

import java.util.concurrent.TimeUnit;

public class XioServerPipeline extends XioBasePipeline {

  private static final int NO_WRITER_IDLE_TIMEOUT = 60000;
  private static final int NO_ALL_IDLE_TIMEOUT = 60000;

  public XioServerPipeline() {
  }

  public XioServerPipeline(XioServerDef def) {
    super(def);
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

  public void addIdleDisconnectHandler(ChannelPipeline pipeline) {
    if (def.getClientIdleTimeout() != null) {
      int timeoutMs = (int) def.getClientIdleTimeout().toMillis();
      ChannelHandler handler = new XioIdleDisconnectHandler(timeoutMs, NO_WRITER_IDLE_TIMEOUT, NO_ALL_IDLE_TIMEOUT, TimeUnit.MILLISECONDS);
      pipeline.addLast("idleDisconnectHandler", handler);
    }
  }

  public String applicationProtocol() {
    return "";
  }
}
