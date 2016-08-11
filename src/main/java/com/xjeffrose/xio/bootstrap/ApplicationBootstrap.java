package com.xjeffrose.xio.bootstrap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ApplicationBootstrap {

  private final ApplicationState state;

  private final Config config;

  private final Map<String, XioServerBootstrap> serverBootstraps = new HashMap<>();

  public ApplicationBootstrap(Config config, ApplicationState state) {
    this.config = config;
    this.state = state;
  }

  public ApplicationBootstrap(Config config) {
    this(config, new ApplicationState(config));
  }

  public ApplicationBootstrap(String application) {
    this(ConfigFactory.load().getConfig(application));
  }

  public ApplicationBootstrap addServer(String server, UnaryOperator<XioServerBootstrap> configure) {
    Config servers = config.getConfig("servers");
    XioServerConfig serverConfig = new XioServerConfig(servers.getConfig(server));
    XioServerState serverState = new XioServerState(servers.getConfig(server));
    XioServerBootstrap serverBootstrap = configure.apply(new XioServerBootstrap(state, serverConfig, serverState).channelConfig(state.getChannelConfiguration()));
    serverBootstraps.put(server, serverBootstrap);
    return this;
  }

  public Application build() {
    Map<String, XioServer> servers = new HashMap<>();
    serverBootstraps.forEach((k, v) -> servers.put(k, v.build()));
    return new Application(servers, state);
  }

}
