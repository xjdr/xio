package com.xjeffrose.xio.proxy;

import com.typesafe.config.Config;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.http.PipelineRouter;
import com.xjeffrose.xio.http.ProxyClientFactory;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.channel.ChannelHandler;
import java.util.Optional;

public class ReverseProxyServer {
  private final String proxyConfig;
  private final String routeConfig;
  private Application application;

  public ReverseProxyServer(String proxyConfig, String routeConfig) {
    this.proxyConfig = proxyConfig;
    this.routeConfig = routeConfig;
  }

  public void start(Config config) {
    ApplicationState appState =
        new ApplicationState(ApplicationConfig.fromConfig(proxyConfig, config));

    ProxyRouteConfig proxyRouteConfig = new ProxyRouteConfig(config.getConfig(routeConfig));

    ProxyClientFactory clientFactory = new ProxyClientFactory(appState);
    RouteStates routeStates = new RouteStates(proxyRouteConfig, appState, clientFactory);

    application =
        new ApplicationBootstrap(appState.config())
            .addServer(
                "main",
                bs ->
                    bs.addToPipeline(
                        new SmartHttpPipeline() {
                          @Override
                          public ChannelHandler getApplicationRouter() {
                            return new PipelineRouter(routeStates.routeMap());
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
