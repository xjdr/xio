package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import lombok.ToString;

@ToString
public class RoutePartial {

  protected final Request request;
  protected final Route route;
  protected final PipelineRequestHandler handler;

  RoutePartial(Request request, Route route, PipelineRequestHandler handler) {
    this.request = request;
    this.route = route;
    this.handler = handler;
  }

  void apply(ChannelHandlerContext ctx) {
    handler.handle(ctx, request, route);
  }
}
