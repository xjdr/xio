package com.xjeffrose.xio.server.trailhead;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.http.*;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.server.Route;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class Trailhead implements AutoCloseable {

  private final RouteConfig routes;
  private final ApplicationBootstrap bootstrap;
  private Application application;

  private RequestHandler buildProvider(Route route, ProxyConfig proxyConfig) {
    return new SimpleProxyHandler(route, proxyConfig);
  }

  private ImmutableMap<Route, RequestHandler> buildProviders() {
    Map<Route, RequestHandler> providers = new LinkedHashMap<>();

    routes.copy().forEach((k, v) -> providers.put(k, buildProvider(k, v)));

    return ImmutableMap.copyOf(providers);
  }

  private XioPipelineFragment proxyFragment() {
    //UrlRouter router = UrlRouter.build(routes, (r) -> buildProviders());
    UrlRouter router = new UrlRouter(buildProviders());
    return new SmartHttpPipeline(new Http1ProxyFragment(router));
  }

  public Trailhead(Config config) {
    routes = RouteConfig.fromConfig("trailhead.routes", config);
    bootstrap = new ApplicationBootstrap("trailhead.application", config)
      .addServer("main", (bs) -> bs.addToPipeline(proxyFragment()))
      ;
  }

  public void start() {
    application = bootstrap.build();
    log.debug("Starting {}", application.getConfig().getName());
  }

  public void close() {
    application.close();
  }

}
