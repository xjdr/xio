package com.xjeffrose.xio.server;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.xjeffrose.xio.core.ShutdownUtil;
import com.xjeffrose.xio.core.XioMetrics;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerTransport;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class XioBootstrap {
  private final ChannelGroup allChannels;
  private final XioServerConfig xioServerConfig;
  private final Map<XioServerDef, XioServerTransport> transports;
  private ExecutorService bossExecutor;
  private ExecutorService workerExecutor;
  private NioServerSocketChannelFactory serverChannelFactory;

  @Inject
  public XioBootstrap(
      Set<XioServerDef> xioServerDefs,
      XioServerConfig xioServerConfig,
      ChannelGroup allChannels) {
    this.allChannels = allChannels;
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
    bossExecutor = xioServerConfig.getBossExecutor();
    workerExecutor = xioServerConfig.getWorkerExecutor();
    serverChannelFactory = new NioServerSocketChannelFactory(bossExecutor, workerExecutor);
    for (XioServerTransport transport : transports.values()) {
      transport.start(serverChannelFactory);
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

    ShutdownUtil.shutdownChannelFactory(serverChannelFactory, bossExecutor, workerExecutor, allChannels);
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