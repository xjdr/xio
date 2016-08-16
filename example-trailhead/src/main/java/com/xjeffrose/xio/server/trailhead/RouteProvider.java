package com.xjeffrose.xio.server.trailhead;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

interface RouteProvider extends AutoCloseable {

  RouteUpdateProvider handle(HttpRequest request, ChannelHandlerContext ctx);

}
