package com.xjeffrose.xio.http;

import com.typesafe.config.ConfigFactory;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class RouteState {
  private final Route route;
  private final PipelineRequestHandler handler;

  public final String path;

  public static Route buildRoute(RouteConfig config) {
    return Route.build(config.path());
  }

  public RouteState(
      Function<RouteConfig, Route> factory, RouteConfig config, PipelineRequestHandler handler) {
    this.route = factory.apply(config);
    this.path = config.path();
    this.handler = handler;
  }

  public RouteState(RouteConfig config, PipelineRequestHandler handler) {
    this(RouteState::buildRoute, config, handler);
  }

  public RouteState(String path, Route route, PipelineRequestHandler handler) {
    this.path = path;
    this.route = route;
    this.handler = handler;
  }

  public static RouteState defaultRoute(PipelineRequestHandler handler) {
    RouteConfig config = new RouteConfig(ConfigFactory.load().getConfig("xio.defaultRoute"));
    return new RouteState((ignored) -> Route.build("*"), config, handler);
  }

  public boolean matches(String path) {
    return route.matches(path);
  }
}
