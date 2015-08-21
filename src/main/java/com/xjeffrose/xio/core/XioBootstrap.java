package com.xjeffrose.xio.core;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * A lifecycle object that manages starting up and shutting down multiple core channels.
 */
public class XioBootstrap {
  private final ChannelGroup allChannels;
  private final NettyServerConfig nettyServerConfig;
  private final Map<HttpServerDef, NettyServerTransport> transports;
  private ExecutorService bossExecutor;
  private ExecutorService workerExecutor;
  private NioServerSocketChannelFactory serverChannelFactory;

  /**
   * This takes a Set of HttpServerDef. Use Guice Multibinder to inject.
   */
  @Inject
  public XioBootstrap(
      Set<HttpServerDef> HttpServerDefs,
      NettyServerConfig nettyServerConfig,
      ChannelGroup allChannels) {
    this.allChannels = allChannels;
    ImmutableMap.Builder<HttpServerDef, NettyServerTransport> builder = new ImmutableMap.Builder<>();
    this.nettyServerConfig = nettyServerConfig;
    for (HttpServerDef HttpServerDef : HttpServerDefs) {
      builder.put(HttpServerDef, new NettyServerTransport(HttpServerDef,
          nettyServerConfig,
          allChannels));
    }
    transports = builder.build();
  }

  @PostConstruct
  public void start() {
    bossExecutor = nettyServerConfig.getBossExecutor();
    workerExecutor = nettyServerConfig.getWorkerExecutor();
    serverChannelFactory = new NioServerSocketChannelFactory(bossExecutor, workerExecutor);
    for (NettyServerTransport transport : transports.values()) {
      transport.start(serverChannelFactory);
    }
  }

  @PreDestroy
  public void stop() {
    for (NettyServerTransport transport : transports.values()) {
      try {
        transport.stop();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    ShutdownUtil.shutdownChannelFactory(serverChannelFactory, bossExecutor, workerExecutor, allChannels);
  }

  public Map<HttpServerDef, Integer> getBoundPorts() {
    ImmutableMap.Builder<HttpServerDef, Integer> builder = new ImmutableMap.Builder<>();
    for (Map.Entry<HttpServerDef, NettyServerTransport> entry : transports.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().getPort());
    }
    return builder.build();
  }

  public Map<HttpServerDef, XioMetrics> getXioMetrics() {
    ImmutableMap.Builder<HttpServerDef, XioMetrics> builder = new ImmutableMap.Builder<>();
    for (Map.Entry<HttpServerDef, NettyServerTransport> entry : transports.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().getMetrics());
    }
    return builder.build();
  }
}