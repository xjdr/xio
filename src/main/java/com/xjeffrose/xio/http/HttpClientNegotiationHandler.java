package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import io.netty.handler.codec.http.HttpClientCodec;

@Slf4j
public class HttpClientNegotiationHandler extends ApplicationProtocolNegotiationHandler {

  private final Supplier<ChannelHandler> http2Handler;

  public HttpClientNegotiationHandler(Supplier<ChannelHandler> http2Handler) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.http2Handler = http2Handler;
  }

  private void replaceCodec(ChannelHandlerContext ctx, ChannelHandler handler) {
    ctx.pipeline().replace(CodecPlaceholderHandler.class, "codec", handler);
  }

  private void replaceApplicationCodec(ChannelHandlerContext ctx, ChannelHandler handler) {
    ctx.pipeline().replace(ApplicationCodecPlaceholderHandler.class, "application codec", handler);
  }

  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
    if (protocol.equals(ApplicationProtocolNames.HTTP_1_1)) {
      replaceCodec(ctx, new HttpClientCodec());
      replaceApplicationCodec(ctx, new Http1ClientCodec());
      ctx.fireUserEventTriggered(RequestBuffer.WriteReady.INSTANCE);
    } else if (protocol.equals(ApplicationProtocolNames.HTTP_2)) {
      replaceCodec(ctx, http2Handler.get());
      replaceApplicationCodec(ctx, new Http2ClientCodec());
    } else {
      throw new RuntimeException("Unknown Application Protocol '" + protocol + "'");
    }
  }
}
