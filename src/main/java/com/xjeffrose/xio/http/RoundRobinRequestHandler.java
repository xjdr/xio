package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableList;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import lombok.Getter;
import lombok.val;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This uses round robin strategy to delegate request handling to another set of one or more request handlers.
 * Currently this is performing the load balancing but should probably merged/reworked with client distributor
 * functionality.
 */
public class RoundRobinRequestHandler implements RequestHandler {
  @Getter
  private final ImmutableList<RequestHandler> handlers;
  private final AtomicInteger next = new AtomicInteger();

  public RoundRobinRequestHandler(ImmutableList<RequestHandler> handlers) {
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
}
