package com.xjeffrose.xio.http;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

public class HttpStatus404Handler implements RequestHandler {

  public HttpStatus404Handler() {
  }

  @Override
  public RequestUpdateHandler handle(HttpRequest request, ChannelHandlerContext ctx) {
    HttpResponse not_found = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
    ctx.writeAndFlush(not_found).addListener(ChannelFutureListener.CLOSE);

    return new RequestUpdateHandler() {
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
