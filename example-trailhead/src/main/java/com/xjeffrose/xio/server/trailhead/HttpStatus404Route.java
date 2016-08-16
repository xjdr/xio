package com.xjeffrose.xio.server.trailhead;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

public class HttpStatus404Route implements RouteProvider {

  public HttpStatus404Route() {
  }

  @Override
  public RouteUpdateProvider handle(HttpRequest request, ChannelHandlerContext ctx) {
    HttpResponse not_found = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
    ctx.writeAndFlush(not_found).addListener(ChannelFutureListener.CLOSE);

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
