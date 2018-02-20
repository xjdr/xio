package com.xjeffrose.xio.http;

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

  public RouteState(
      Function<RouteConfig, Route> factory, RouteConfig config, PipelineRequestHandler handler) {
    this.config = config;
    route = factory.apply(config);
    this.handler = handler;
  }

  public RouteState(RouteConfig config, PipelineRequestHandler handler) {
    this(RouteState::buildRoute, config, handler);
  }
}
