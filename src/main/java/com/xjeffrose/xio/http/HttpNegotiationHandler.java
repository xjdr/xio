package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpNegotiationHandler extends ApplicationProtocolNegotiationHandler {

  public HttpNegotiationHandler() {
    super(ApplicationProtocolNames.HTTP_1_1);
  }

  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
    if (protocol.equals(ApplicationProtocolNames.HTTP_1_1)) {
      ctx.pipeline().replace(CodecPlaceholderHandler.class, "codec", new HttpServerCodec());
    } else if (protocol.equals(ApplicationProtocolNames.HTTP_2) && false) {
      // TODO(CK): implement http 2
    } else {
      throw new RuntimeException("Unknown Application Protocol '" + protocol + "'");
    }
  }

}
