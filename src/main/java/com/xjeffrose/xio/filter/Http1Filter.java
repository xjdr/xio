package com.xjeffrose.xio.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;

public class Http1Filter extends ChannelInboundHandlerAdapter {

  private final Http1FilterConfig config;

  public Http1Filter(Http1FilterConfig config) {
    this.config = config;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) msg;
      filter(ctx, request);
    }
    ctx.fireChannelRead(msg);
  }

  /**
   * If there is no request or the request is denied close the ctx. Otherwise allow the connection
   * to live. In either case remove this handler from the pipeline.
   */
  private void filter(ChannelHandlerContext ctx, HttpRequest request) {
    ctx.pipeline().remove(this);
    if (request == null || config.denied(request)) {
      ctx.close();
    }
  }
}
