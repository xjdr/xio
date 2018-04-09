package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap;
import java.util.Map;

// TODO(CK): both this and Route.java need to be refactored so that the load order of routes
// doesn't matter. Off the top of my head this means parsing the routes into some sort of tree
// data structure and walking the tree looking for hits.
public class PathToRequestHandler {

  private final ImmutableMap<String, RouteState> routes;
  private final Map.Entry<String, RouteState> defaultEntry;

  public PathToRequestHandler(
      ImmutableMap<String, RouteState> routes, PipelineRequestHandler defaultHandler) {
    this.routes = routes;
    RouteState defaultRoute = RouteState.defaultRoute(defaultHandler);
    defaultEntry = new AbstractMap.SimpleEntry("*", defaultRoute);
  }

  public PathToRequestHandler(PipelineRequestHandler handler) {
    this(ImmutableMap.of(), handler);
  }

  public PathToRequestHandler(ImmutableMap<String, RouteState> routes) {
    this(routes, new Status404RequestHandler());
  }

  public Map.Entry<String, RouteState> lookup(Request request) {
    for (Map.Entry<String, RouteState> entry : routes.entrySet()) {
      RouteState route = entry.getValue();
      if (route.matches(request.path())) {
        return entry;
      }
    }

    return defaultEntry;
  }
}
