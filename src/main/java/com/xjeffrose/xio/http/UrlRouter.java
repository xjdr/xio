package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.server.Route;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;

import java.util.function.Function;

public class UrlRouter {

  private RouteProvider determineRoute(HttpRequest request) {
    for(Route route : routes.keySet()) {
      if (route.matches(request.uri())) {
        return routes.get(route);
      }
    }
    return defaultRoute;
  }

  //private final RouteConfig config;
  private final ImmutableMap<Route, RouteProvider> routes;
  private final RouteProvider defaultRoute;

  /*
  private UrlRouter(RouteConfig config, ImmutableMap<Route, RouteProvider> routes) {
    this.config = config;
    this.routes = routes;
    defaultRoute = new HttpStatus404Route();
  }
  */

  public UrlRouter(ImmutableMap<Route, RouteProvider> routes) {
    this.routes = routes;
    defaultRoute = new HttpStatus404Route();
  }

  /*
  public static UrlRouter build(RouteConfig config, Function<RouteConfig, ImmutableMap<Route, RouteProvider>> builder) {
    return new UrlRouter(config, builder.apply(config));
  }
  */

  public RouteProvider get(HttpRequest request) {
    return determineRoute(request);
  }

}
