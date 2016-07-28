package com.xjeffrose.xio.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;


public class XioHttp2UpgradeHandler extends ApplicationProtocolNegotiationHandler {

  private XioChannelHandlerFactory handlerFactory;

  public XioHttp2UpgradeHandler(XioChannelHandlerFactory handlerFactory) {
    this();
    this.handlerFactory = handlerFactory;
  }

  protected XioHttp2UpgradeHandler() {
    super(ApplicationProtocolNames.HTTP_1_1);
  }

  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      ctx.pipeline().addAfter(ctx.name(), "Http2Handler", new Http2MultiplexCodec(true, handlerFactory.getHandler(), ctx.channel().eventLoop()));
      ctx.pipeline().remove(ctx.handler());
      return;
    }

    if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      ctx.pipeline().addAfter(ctx.name(), "HttpServerCodec", new HttpServerCodec());
      ctx.pipeline().remove(ctx.handler());
      return;
    }

    throw new IllegalStateException("unknown protocol: " + protocol);
  }
}
