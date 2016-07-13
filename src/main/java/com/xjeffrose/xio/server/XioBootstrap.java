package com.xjeffrose.xio.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ExecutionError;
import com.google.inject.Inject;
import com.xjeffrose.xio.core.XioMetrics;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;


public class XioBootstrap {
  private static final Logger log = Logger.getLogger(XioServerTransport.class.getName());

  private final XioServerConfig xioServerConfig;
  private Map<XioServerDef, XioServerTransport> transports;
  private NioEventLoopGroup bossGroup;
  private NioEventLoopGroup workerGroup;
  private final ChannelGroup allChannels;

  @Inject
  public XioBootstrap(
      Set<XioServerDef> xioServerDefs,
      XioServerConfig xioServerConfig,
      ChannelGroup allChannels) {
    ImmutableMap.Builder builder = ImmutableMap.builder();
    this.xioServerConfig = xioServerConfig;
    this.allChannels = allChannels;
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
      try {
        transport.start(bossGroup, workerGroup);
      } catch (ExecutionError e) {
        log.error("Error starting port " + transport.getPort());
        throw new RuntimeException(e);
      }
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

  public void stopPort(int port){
    ImmutableMap.Builder builder = ImmutableMap.builder();
    for (XioServerDef key : transports.keySet()) {
      XioServerTransport v = transports.get(key);
      if(v.getPort() == port){
        try {
          v.stop();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      else{
        builder.put(key,v);
      }
    }
    transports = builder.build();
  }

  public void startServer(XioServerDef serverDef){
    XioServerTransport transport = new  XioServerTransport(serverDef,
            xioServerConfig,
            allChannels);
    try {
      transport.start(bossGroup, workerGroup);
    } catch (ExecutionError e) {
      log.error("Error starting port " + transport.getPort());
      throw new RuntimeException(e);
    }
    ImmutableMap.Builder builder = ImmutableMap.builder().putAll(transports).put(serverDef,transport);
    transports = builder.build();
  }
}