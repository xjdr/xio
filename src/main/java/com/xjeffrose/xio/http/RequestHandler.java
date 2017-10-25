package com.xjeffrose.xio.http;

import com.xjeffrose.xio.server.Route;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

public interface RequestHandler extends AutoCloseable {
  RequestUpdateHandler handle(HttpRequest request, ChannelHandlerContext ctx);
}
