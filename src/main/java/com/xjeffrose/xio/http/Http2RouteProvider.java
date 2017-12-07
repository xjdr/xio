package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;

// TODO(CK): Refactor this after we find a way to unify HTTP/1 and HTTP/2
public interface Http2RouteProvider {

  // TODO(CK): ChannelHandlerContext should come first
  void handle(Http2Request request, ChannelHandlerContext ctx);

  /**
   * The channel has been closed, cleanup any unused resources
   */
  void close(ChannelHandlerContext ctx);

}
