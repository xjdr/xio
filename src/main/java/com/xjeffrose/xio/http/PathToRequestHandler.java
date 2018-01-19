package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap;
import java.util.Map;

// TODO(CK): both this and Route.java need to be refactored so that the load order of routes
// doesn't matter. Off the top of my head this means parsing the routes into some sort of tree
// data structure and walking the tree looking for hits.
public class PathToRequestHandler {

  private final ImmutableMap<Route, PipelineRequestHandler> routes;
  private final Map.Entry<Route, PipelineRequestHandler> defaultEntry;

  public PathToRequestHandler(
      ImmutableMap<Route, PipelineRequestHandler> routes, PipelineRequestHandler defaultHandler) {
    this.routes = routes;
    defaultEntry = new AbstractMap.SimpleEntry(Route.build("*"), defaultHandler);
  }

  public PathToRequestHandler(PipelineRequestHandler handler) {
    this(ImmutableMap.of(), handler);
  }

  public PathToRequestHandler(ImmutableMap<Route, PipelineRequestHandler> routes) {
    this(routes, new Status404RequestHandler());
  }

  public Map.Entry<Route, PipelineRequestHandler> lookup(Request request) {
    for (Map.Entry<Route, PipelineRequestHandler> entry : routes.entrySet()) {
      Route route = entry.getKey();
      if (route.matches(request.path())) {
        return entry;
      }
    }

    return defaultEntry;
  }
}
