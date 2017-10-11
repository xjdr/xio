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
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XioHttp404Handler extends SimpleChannelInboundHandler<Object> {
  private static final Logger log = LoggerFactory.getLogger(XioHttp404Handler.class);

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HTTP_1_1,
      NOT_FOUND,
      Unpooled.copiedBuffer("", CharsetUtil.UTF_8)
    );
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

    // Write the response.
    ctx.write(response);
    ctx.flush();
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object object) {
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("exception caught!", cause);
    ctx.close();
  }
}
