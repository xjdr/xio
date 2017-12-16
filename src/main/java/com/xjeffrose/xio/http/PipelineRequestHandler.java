package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;

public interface PipelineRequestHandler {

  void handle(ChannelHandlerContext ctx, Request request, Route route);

}
