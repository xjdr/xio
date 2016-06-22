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
  private final XioSecurityHandlers securityHandlers;

  public XioServerPipeline(XioServerDef def, ZkClient zkClient, XioServerConfig xioServerConfig, ChannelStatistics channelStatistics) {
    super(def, zkClient, xioServerConfig, channelStatistics);
    securityHandlers = def.getSecurityFactory().getSecurityHandlers(def, xioServerConfig);
  }

  public ChannelHandler getEncryptionHandler() {
    return securityHandlers.getEncryptionHandler();
  }

  public ChannelHandler getAuthenticationHandler() {
    return securityHandlers.getAuthenticationHandler();
  }

  public void addIdleDisconnectHandler(ChannelPipeline pipeline) {
    if (def.getClientIdleTimeout() != null) {
      int timeoutMs = (int) def.getClientIdleTimeout().toMillis();
      ChannelHandler handler = new XioIdleDisconnectHandler(timeoutMs, NO_WRITER_IDLE_TIMEOUT, NO_ALL_IDLE_TIMEOUT, TimeUnit.MILLISECONDS);
      pipeline.addLast("idleDisconnectHandler", handler);
    }
  }

}
