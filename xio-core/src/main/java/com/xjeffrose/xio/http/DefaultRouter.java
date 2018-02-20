package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultRouter implements Router {
  @Override
  public RequestHandler get(HttpRequest request) {
    return new RequestHandler() {
      @Override
      public RequestUpdateHandler handle(HttpRequest request, ChannelHandlerContext ctx) {
        log.warn("No router has been configured yet!");
        return new RequestUpdateHandler() {
          @Override
          public void update(HttpContent content) {}

          @Override
          public void update(LastHttpContent last) {}
        };
      }

      @Override
      public void close() throws Exception {}
    };
  }
}
