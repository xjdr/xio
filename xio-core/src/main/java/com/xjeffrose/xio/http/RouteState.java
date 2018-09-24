package com.xjeffrose.xio.http;

import com.typesafe.config.ConfigFactory;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class RouteState {
  private final RouteConfig config;
  private final Route route;
  private final PipelineRequestHandler handler;

  public static Route buildRoute(RouteConfig config) {
    return Route.build(config.path());
  }

  // copy constructor
  public RouteState(RouteState routeState) {
    this.config = routeState.config;
    this.route = routeState.route;
    this.handler = routeState.handler;
  }

  public RouteState(
      Function<RouteConfig, Route> factory, RouteConfig config, PipelineRequestHandler handler) {
    this.config = config;
    route = factory.apply(config);
    this.handler = handler;
  }

  public RouteState(RouteConfig config, PipelineRequestHandler handler) {
    this(RouteState::buildRoute, config, handler);
  }

  public static RouteState defaultRoute(PipelineRequestHandler handler) {
    RouteConfig config = new RouteConfig(ConfigFactory.load().getConfig("xio.defaultRoute"));
    return new RouteState((ignored) -> Route.build("*"), config, handler);
  }

  public boolean matches(String path) {
    return route.matches(path);
  }

  public String path() {
    return config.path();
  }
}
