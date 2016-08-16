package com.xjeffrose.xio.bootstrap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ApplicationBootstrap {

  private final ApplicationConfig config;

  private final ApplicationState state;

  private final Map<String, XioServerBootstrap> serverBootstraps = new HashMap<>();

  public ApplicationBootstrap(ApplicationConfig config, ApplicationState state) {
    this.config = config;
    this.state = state;
  }

  public ApplicationBootstrap(Config config) {
    this(new ApplicationConfig(config), new ApplicationState(config));
  }

  public ApplicationBootstrap(String key, Config config) {
    this(config.getConfig(key));
  }

  public ApplicationBootstrap(String application) {
    this(application, ConfigFactory.load());
  }

  public ApplicationBootstrap addServer(String server, UnaryOperator<XioServerBootstrap> configure) {
    XioServerConfig serverConfig = new XioServerConfig(config.getServer(server));
    XioServerState serverState = new XioServerState(config.getServer(server));
    XioServerBootstrap serverBootstrap = configure.apply(new XioServerBootstrap(state, serverConfig, serverState).channelConfig(state.getChannelConfiguration()));
    serverBootstraps.put(server, serverBootstrap);
    return this;
  }

  public Application build() {
    Map<String, XioServer> servers = new HashMap<>();
    serverBootstraps.forEach((k, v) -> servers.put(k, v.build()));
    return new Application(config, servers, state);
  }

}
