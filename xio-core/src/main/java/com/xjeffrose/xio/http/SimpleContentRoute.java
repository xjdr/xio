package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleContentRoute implements RequestHandler {

  private final ContentConfig config;

  public SimpleContentRoute(ContentConfig config) {
    this.config = config;
  }

  @Override
  public RequestUpdateHandler handle(HttpRequest request, ChannelHandlerContext ctx) {
    if (HttpUtil.is100ContinueExpected(request)) {
      ctx.writeAndFlush(
          new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
    }

    return new RequestUpdateHandler() {
      @Override
      public void update(HttpContent content) {}

      @Override
      public void update(LastHttpContent last) {}
    };
  }

  @Override
  public void close() {}
}
