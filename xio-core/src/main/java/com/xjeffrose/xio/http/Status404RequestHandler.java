package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;

public class Status404RequestHandler implements PipelineRequestHandler {

  @Override
  public void handle(ChannelHandlerContext ctx, Request request, RouteState route) {
    Response notFound = ResponseBuilders.newNotFound(request);
    ctx.writeAndFlush(notFound);
  }
}
