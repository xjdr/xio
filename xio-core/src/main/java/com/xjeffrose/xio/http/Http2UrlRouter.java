package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.server.Route;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2UrlRouter {

  private Http2RouteProvider determineRoute(Http2Headers requestHeaders) {
    log.debug("Trying to determine route for {}", requestHeaders);
    for (Route route : routes.keySet()) {
      log.debug("Testing route {}", route);
      if (route.matches(requestHeaders.path().toString())) {
        return routes.get(route);
      }
    }
    log.debug("Gave up, yielding default route");
    return defaultRoute;
  }

  private final ImmutableMap<Route, Http2RouteProvider> routes;
  private final Http2RouteProvider defaultRoute;

  public Http2UrlRouter(ImmutableMap<Route, Http2RouteProvider> routes) {
    this.routes = routes;
    defaultRoute = new Http2Status404Route();
  }

  public Http2RouteProvider get(Http2Headers requestHeaders) {
    return determineRoute(requestHeaders);
  }

  public static class Http2Status404Route implements Http2RouteProvider {

    @Override
    public void handle(Http2Request request, ChannelHandlerContext ctx) {
      Http2Headers headers =
          new DefaultHttp2Headers().status(HttpResponseStatus.NOT_FOUND.codeAsText());
      Http2Response<Http2Headers> not_found = Http2Response.build(request.streamId, headers, true);
      ctx.writeAndFlush(not_found);
    }

    @Override
    public void close(ChannelHandlerContext ctx) {}
  }
}
