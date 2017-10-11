package com.xjeffrose.xio.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.ssl.OptionalSslHandler;
import io.netty.util.CharsetUtil;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

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
