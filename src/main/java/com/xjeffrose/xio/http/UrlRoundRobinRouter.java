package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.client.XioClientBootstrap;
import com.xjeffrose.xio.core.ConfigurationUpdater;
import com.xjeffrose.xio.filter.IpFilterConfig;
import com.xjeffrose.xio.server.Route;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpRequest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class UrlRoundRobinRouter implements HttpRouter {
  private ImmutableMap<Route, ImmutableList<Host>> routes;
  private final RouteProvider defaultRoute = new HttpStatus404Route();
  private final AtomicInteger next = new AtomicInteger();
  private final Function<Boolean, ChannelHandler> tracingHandler;

  public UrlRoundRobinRouter() {
    this.routes = ImmutableMap.of();
    this.tracingHandler = null;
  }
  public UrlRoundRobinRouter(
    ImmutableMap<Route, ImmutableList<Host>> routes,
    Function<Boolean, ChannelHandler> tracingHandler) {
    log.info("New UrlRoundRobinRouter {}", routes);
    this.routes = routes;
    this.tracingHandler = tracingHandler;
  }

  @Override
  public RouteProvider get(HttpRequest request) {
    log.info("selectRouteHosts routes.keySet={}", routes.keySet());
    Route selectedRoute = null;
    ImmutableList<Host> hosts = null;
    for (Route route : routes.keySet()) {
      log.info("route: {}", route.pathPattern());
      if (route.matches(request.uri())) {
        selectedRoute = route;
        hosts = routes.get(route);
        break;
      }
    }
    log.info("selectHostRouteProvider hosts={}", hosts);
    if (selectedRoute == null) {
      return defaultRoute;
    }
    int idx = next.getAndIncrement();
    val host = hosts.get(idx % hosts.size());
    val proxyConfig = new ProxyConfig(host.getAddress(), host.getHostHeader(), "", "/", host.isNeedSSL());
    val client = new XioClientBootstrap();
    if (tracingHandler != null) {
      client.tracingHandler(() -> tracingHandler.apply(host.isNeedSSL()));
    }
    return new SimpleProxyRoute(selectedRoute, proxyConfig, client);
  }
}

