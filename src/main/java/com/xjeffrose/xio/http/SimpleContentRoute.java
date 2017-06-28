package com.xjeffrose.xio.http;

import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioClientBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimpleContentRoute implements RouteProvider {

  private final ContentConfig config;

  public SimpleContentRoute(ContentConfig config) {
    this.config = config;
  }

  @Override
  public RouteUpdateProvider handle(HttpRequest request, ChannelHandlerContext ctx) {
    if (HttpUtil.is100ContinueExpected(request)) {
      ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
    }

    return new RouteUpdateProvider() {
      @Override
      public void update(HttpContent content) {
      }
      @Override
      public void update(LastHttpContent last) {
      }
    };
  }

  @Override
  public void close() {
  }

}
