package com.xjeffrose.xio.bootstrap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.pipeline.XioPipelineAssembler;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerInstrumentation;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class XioServerBootstrap {
  private static final Logger log = LoggerFactory.getLogger(XioServerBootstrap.class);

  private final ServerBootstrap serverBootstrap;

  private final XioPipelineAssembler pipelineAssembler;

  private final XioServerConfig config;

  private final XioServerState state;

  private ServerChannelConfiguration channelConfig;

  public XioServerBootstrap(ApplicationState appState, XioServerConfig config, XioServerState state) {
    serverBootstrap = new ServerBootstrap();
    pipelineAssembler = new XioPipelineAssembler(appState, config, state);
    this.config = config;
    this.state = state;
    bindAddress(config.getBindAddress());
    channelConfig(appState.getChannelConfiguration());
  }

  static public XioServerBootstrap fromConfig(String key, Config config) {
    Config servers = config.getConfig(key).getConfig("servers");
    String firstServer = servers.root().entrySet().iterator().next().getKey();
    return new XioServerBootstrap(
      ApplicationState.fromConfig(key, config),
      XioServerConfig.fromConfig(firstServer, servers),
      XioServerState.fromConfig(firstServer, servers)
    );
  }

  static public XioServerBootstrap fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

  public XioServerBootstrap addToPipeline(XioPipelineFragment fragment) {
    // TODO(CK): interrogate fragment for channel options
    pipelineAssembler.addFragment(fragment);
    return this;
  }

  public XioServerBootstrap bindAddress(InetSocketAddress address) {
    serverBootstrap.localAddress(address);
    return this;
  }

  public XioServerBootstrap channelConfig(ServerChannelConfiguration channelConfig) {
    this.channelConfig = channelConfig;
    return this;
  }

  public XioServer build() {
    log.debug("Building");
    serverBootstrap.group(channelConfig.bossGroup(), channelConfig.workerGroup());
    serverBootstrap.channel(channelConfig.channel());
    final XioServerInstrumentation instrumentation = new XioServerInstrumentation();
    serverBootstrap.childHandler(pipelineAssembler.build(instrumentation));
    ChannelFuture future = serverBootstrap.bind();
    future.awaitUninterruptibly();
    if (future.isSuccess()) {
      instrumentation.addressBound = (InetSocketAddress)future.channel().localAddress();
    } else {
      log.error("Couldn't bind channel", future.cause());
      throw new RuntimeException(future.cause());
    }
    return new XioServer(future.channel(), instrumentation, config, state);
  }
}
