package com.xjeffrose.xio.http;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

public class Status404RequestHandler implements PipelineRequestHandler {

  @Override
  public void handle(ChannelHandlerContext ctx, Request request, Route route) {
    Response notFound = ResponseBuilders.newNotFound();
    ctx.writeAndFlush(notFound);
  }

}
