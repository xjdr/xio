package com.xjeffrose.xio.proxy;

import com.typesafe.config.Config;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.http.PipelineRouter;
import com.xjeffrose.xio.http.ProxyClientFactory;
import com.xjeffrose.xio.http.ProxyRouteConfig;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ReverseProxyServer {
  private final String proxyConfig;
  private final String routeConfig;
  private Application application;
  private final AtomicInteger requestCount = new AtomicInteger(0);

  public ReverseProxyServer(String proxyConfig, String routeConfig) {
    this.proxyConfig = proxyConfig;
    this.routeConfig = routeConfig;
  }

  public void start(Config config) {
    ApplicationState appState =
        new ApplicationState(ApplicationConfig.fromConfig(proxyConfig, config));

    Stream<ProxyRouteConfig> proxyRouteConfigs =
        Arrays.stream(routeConfig.split(",")).map(it -> new ProxyRouteConfig(config.getConfig(it)));

    ProxyClientFactory clientFactory = new ProxyClientFactory(appState);
    RouteStates routeStates = new RouteStates(proxyRouteConfigs, appState, clientFactory);

    application =
        new ApplicationBootstrap(appState.config())
            .addServer(
                "main",
                bs ->
                    bs.addToPipeline(
                        new SmartHttpPipeline() {
                          @Override
                          public ChannelHandler getApplicationRouter() {
                            return new PipelineRouter(routeStates.routeMap()) {
                              @Override
                              protected void channelRead0(ChannelHandlerContext ctx, Request msg) {
                                super.channelRead0(ctx, msg);
                                if (msg.endOfMessage()) {
                                  requestCount.incrementAndGet();
                                }
                              }
                            };
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

  public int getRequestCount() {
    return requestCount.get();
  }
}
