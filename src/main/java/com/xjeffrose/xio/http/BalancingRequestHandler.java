package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableList;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import lombok.Getter;
import lombok.val;

import java.util.concurrent.atomic.AtomicInteger;

public class BalancingRequestHandler implements RequestHandler {
  @Getter
  private final ImmutableList<RequestHandler> handlers;
  private final AtomicInteger next = new AtomicInteger();

  public BalancingRequestHandler(ImmutableList<RequestHandler> handlers) {
    this.handlers = handlers;
  }

  @Override
  public RequestUpdateHandler handle(HttpRequest request, ChannelHandlerContext ctx) {
    int idx = next.getAndIncrement();
    val handler = handlers.get(idx % handlers.size());
    return handler.handle(request, ctx);
  }

  @Override
  public void close() throws Exception {
    handlers.forEach(h -> {
      if (h == null) return;
      try {
        h.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

//  @Override
//  public String toString() {
//    return "BalancingRequestHandler{" +
//      "handlers=" + handlers +
//      ", next=" + next +
//      '}';
//  }
}
