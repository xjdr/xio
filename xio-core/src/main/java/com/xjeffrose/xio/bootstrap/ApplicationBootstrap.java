package com.xjeffrose.xio.bootstrap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.application.XioServiceLocator;
import com.xjeffrose.xio.config.Configurator;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.filter.Http1FilterConfig;
import com.xjeffrose.xio.filter.IpFilterConfig;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import com.xjeffrose.xio.tracing.XioTracing;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.Getter;

public class ApplicationBootstrap {

  // TODO(CK): Make this configurable
  /** ApplicationRunner knows how to stop a running Application */
  public static class ApplicationRunner {
    private final Application app;

    ApplicationRunner(Application app) {
      this.app = app;
    }

    public void start() {
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread() {
                @Override
                public void run() {
                  stop();
                }
              });
    }

    public void stop() {
      app.close();
    }
  }

  @Getter private final ApplicationConfig config;

  private final ApplicationState state;

  private final Map<String, XioServerBootstrap> serverBootstraps = new HashMap<>();

  public ApplicationBootstrap(ApplicationConfig config, ApplicationState state) {
    this.config = config;
    this.state = state;

    XioServiceLocator.INSTANCE = new XioServiceLocator(config, state);
  }

  public ApplicationBootstrap(ApplicationConfig config) {
    this(config, new ApplicationState(config));
  }

  public ApplicationBootstrap(ApplicationState state) {
    this(state.config(), state);
  }

  public ApplicationBootstrap(Config config) {
    this(new ApplicationConfig(config));
  }

  public ApplicationBootstrap(String key, Config config) {
    this(config.getConfig(key));
  }

  public ApplicationBootstrap(String application) {
    this(application, ConfigFactory.load());
  }

  public ApplicationBootstrap(String application, Function<Config, XioTracing> tracingSupplier) {
    this(new ApplicationConfig(ConfigFactory.load().getConfig(application), tracingSupplier));
  }

  public ApplicationBootstrap addServer(
      String server, UnaryOperator<XioServerBootstrap> configure) {
    XioServerConfig serverConfig = new XioServerConfig(config.getServer(server));
    XioServerState serverState = new XioServerState(serverConfig);
    XioServerBootstrap serverBootstrap =
        configure.apply(
            new XioServerBootstrap(state, serverConfig, serverState)
                .channelConfig(state.getChannelConfiguration()));
    serverBootstraps.put(server, serverBootstrap);
    return this;
  }

  public Application build() {
    Map<String, XioServer> servers = new HashMap<>();
    serverBootstraps.forEach((k, v) -> servers.put(k, v.build()));

    ZkClient zkClient = state.getZkClient();
    zkClient.registerUpdater(
        new IpFilterConfig.Updater(config.getIpFilterPath(), state::setIpFilterConfig));
    zkClient.registerUpdater(
        new Http1FilterConfig.Updater(config.getHttp1FilterPath(), state::setHttp1FilterConfig));
    state.getZkClient().start();

    Configurator configurator = Configurator.build(config.settings());
    configurator.start();
    Application application = new Application(config, servers, state, configurator);
    new ApplicationRunner(application).start();
    return application;
  }
}
