package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

// TODO RouteProvider -> RequestHandler
public interface RouteProvider extends AutoCloseable {

  // TODO RouteUpdateProvider -> RequestUpdateHandler
  RouteUpdateProvider handle(HttpRequest request, ChannelHandlerContext ctx);

}
