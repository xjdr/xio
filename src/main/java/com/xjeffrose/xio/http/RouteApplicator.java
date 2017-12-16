package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RouteApplicator extends SimpleChannelInboundHandler<RoutePartial> {

  @Override
  public void channelRead0(ChannelHandlerContext ctx, RoutePartial msg) throws Exception {
    log.debug("RouteApplicator {}", msg);
    msg.apply(ctx);
  }

}
