package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

public interface RequestHandler extends AutoCloseable {
  RequestUpdateHandler handle(HttpRequest request, ChannelHandlerContext ctx);
}
