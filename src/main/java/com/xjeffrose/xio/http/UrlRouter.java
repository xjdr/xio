package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.server.Route;
import io.netty.handler.codec.http.HttpRequest;
import lombok.Getter;


public class UrlRouter implements Router {

  @Getter
  private final ImmutableMap<Route, RequestHandler> routes;
  private final RequestHandler defaultRoute;

  public UrlRouter(ImmutableMap<Route, RequestHandler> routes) {
    this.routes = routes;
    defaultRoute = new HttpStatus404Handler();
  }

  public RequestHandler get(HttpRequest request) {
    return determineRoute(request);
  }

  private RequestHandler determineRoute(HttpRequest request) {
    for (Route route : routes.keySet()) {
      if (route.matches(request.uri())) {
        return routes.get(route);
      }
    }
    return defaultRoute;
  }
}
