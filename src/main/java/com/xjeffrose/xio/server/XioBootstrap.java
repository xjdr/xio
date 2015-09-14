package com.xjeffrose.xio.server;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.xjeffrose.xio.core.XioMetrics;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


public class XioBootstrap {
  private final XioServerConfig xioServerConfig;
  private final Map<XioServerDef, XioServerTransport> transports;
  private NioEventLoopGroup bossGroup;
  private NioEventLoopGroup workerGroup;

  @Inject
  public XioBootstrap(
      Set<XioServerDef> xioServerDefs,
      XioServerConfig xioServerConfig,
      ChannelGroup allChannels) {
    ImmutableMap.Builder<XioServerDef, XioServerTransport> builder = new ImmutableMap.Builder<>();
    this.xioServerConfig = xioServerConfig;
    for (XioServerDef XioServerDef : xioServerDefs) {
      builder.put(XioServerDef, new XioServerTransport(XioServerDef,
          xioServerConfig,
          allChannels));
    }
    transports = builder.build();
  }

  @PostConstruct
  public void start() {
    bossGroup = new NioEventLoopGroup(xioServerConfig.getBossThreadCount());
    workerGroup = new NioEventLoopGroup(xioServerConfig.getWorkerThreadCount());
    for (XioServerTransport transport : transports.values()) {
      transport.start(bossGroup, workerGroup);
    }
  }

  @PreDestroy
  public void stop() {
    for (XioServerTransport transport : transports.values()) {
      try {
        transport.stop();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
  }

  public Map<XioServerDef, Integer> getBoundPorts() {
    ImmutableMap.Builder<XioServerDef, Integer> builder = new ImmutableMap.Builder<>();
    for (Map.Entry<XioServerDef, XioServerTransport> entry : transports.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().getPort());
    }
    return builder.build();
  }

  public Map<XioServerDef, XioMetrics> getXioMetrics() {
    ImmutableMap.Builder<XioServerDef, XioMetrics> builder = new ImmutableMap.Builder<>();
    for (Map.Entry<XioServerDef, XioServerTransport> entry : transports.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().getMetrics());
    }
    return builder.build();
  }
}