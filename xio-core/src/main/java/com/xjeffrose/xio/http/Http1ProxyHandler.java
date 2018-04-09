package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http1ProxyHandler extends SimpleChannelInboundHandler<HttpObject> {

  private final Router router;
  private RequestHandler route;
  private RequestUpdateHandler updater;

  public Http1ProxyHandler(Router router) {
    this.router = router;
  }

  @Override
  public final void channelRead0(final ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest req = (HttpRequest) msg;
      log.info("Received Request {}", req);
      route = router.get(req);
      updater = route.handle(req, ctx);
    } else if (msg instanceof LastHttpContent) {
      updater.update((LastHttpContent) msg);
    } else if (msg instanceof HttpContent) {
      updater.update((HttpContent) msg);
    }
  }

  @Override
  public final void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (route != null) {
      route.close();
    }
  }

  @Override
  public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("exceptionCaught", cause);
    ctx.close();
  }
}
