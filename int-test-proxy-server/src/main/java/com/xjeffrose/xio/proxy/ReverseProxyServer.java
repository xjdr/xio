package com.xjeffrose.xio.proxy;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.http.*;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.channel.ChannelHandler;
import java.util.Optional;

public class ReverseProxyServer {
  private final boolean useH2;
  private Application application;

  public ReverseProxyServer(boolean useH2) {
    this.useH2 = useH2;
  }

  private String proxyConfig() {
    if (useH2) {
      return "xio.h2ReverseProxy";
    } else {
      return "xio.h1ReverseProxy";
    }
  }

  public void start() {
    Config config = ConfigFactory.load();
    ApplicationState appState =
        new ApplicationState(ApplicationConfig.fromConfig(proxyConfig(), config));

    ProxyRouteConfig proxyRouteConfig =
        new ProxyRouteConfig(config.getConfig("xio.testProxyRoute"));

    ClientFactory clientFactory = new ProxyClientFactory(appState);

    application =
        new ApplicationBootstrap(appState.config())
            .addServer(
                "main",
                bs ->
                    bs.addToPipeline(
                        new SmartHttpPipeline() {
                          @Override
                          public ChannelHandler getApplicationRouter() {
                            return new PipelineRouter(
                                ImmutableMap.of(),
                                new ProxyHandler(
                                    clientFactory, proxyRouteConfig, new SocketAddressHelper()));
                          }
                        }))
            .build();
  }

  public void stop() {
    Optional.ofNullable(application).ifPresent(Application::close);
    application = null;
  }

  public int port() {
    return Optional.ofNullable(application)
        .map(app -> app.instrumentation("main").boundAddress().getPort())
        .orElse(-1);
  }
}
