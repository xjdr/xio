package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Map;

public class PipelineRouter extends SimpleChannelInboundHandler<Request> {

  private final PathToRequestHandler requestHandlers;

  public PipelineRouter(
      ImmutableMap<Route, PipelineRequestHandler> routes, PipelineRequestHandler defaultHandler) {
    requestHandlers = new PathToRequestHandler(routes, defaultHandler);
  }

  public PipelineRouter(PathToRequestHandler requestHandlers) {
    this.requestHandlers = requestHandlers;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Request msg) {
    Map.Entry<Route, PipelineRequestHandler> entry = requestHandlers.lookup(msg);

    ctx.fireChannelRead(new RoutePartial(msg, entry.getKey(), entry.getValue()));
  }
}
