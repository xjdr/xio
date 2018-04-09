package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XioHttp404Handler extends SimpleChannelInboundHandler<Object> {
  private static final Logger log = LoggerFactory.getLogger(XioHttp404Handler.class);

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    FullHttpResponse response =
        new DefaultFullHttpResponse(
            HTTP_1_1, NOT_FOUND, Unpooled.copiedBuffer("", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

    // Write the response.
    ctx.write(response);
    ctx.flush();
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object object) {}

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("exception caught!", cause);
    ctx.close();
  }
}
