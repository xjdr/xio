package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@ToString
@Accessors(fluent = true)
@Getter
public class RoutePartial {

  private final Request request;
  private final String path;
  private final RouteState route;

  RoutePartial(Request request, String path, RouteState route) {
    this.request = request;
    this.path = path;
    this.route = route;
  }

  void apply(ChannelHandlerContext ctx) {
    route.handler().handle(ctx, request, route);
  }
}
