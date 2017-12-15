package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Map;
import io.netty.channel.ChannelHandlerContext;
import java.util.AbstractMap;

public class PathToRequestHandler {

  private final ImmutableMap<Route, PipelineRequestHandler> routes;
  private final Map.Entry<Route, PipelineRequestHandler> defaultEntry;

  public PathToRequestHandler(ImmutableMap<Route, PipelineRequestHandler> routes, PipelineRequestHandler defaultHandler) {
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
