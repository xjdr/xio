package com.xjeffrose.xio.firewall;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.xjeffrose.xio.server.ServerLimits;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ConnectionLimiter extends ChannelDuplexHandler {
  private final AtomicInteger numConnections;
  private final int maxConnections;
  private final Counter connections;

  public ConnectionLimiter(MetricRegistry metrics, ServerLimits limits) {
    this.maxConnections = limits.maxConnections();
    this.numConnections = new AtomicInteger(0);
    this.connections = metrics.counter(name("Active Connections"));
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    connections.inc();

    // TODO(JR): Should this return a 429 or is the current logic of silently dropping the
    // connection sufficient?
    if (maxConnections > 0) {
      if (numConnections.incrementAndGet() > maxConnections) {
        log.info("Accepted connection above limit {}. Dropping.", maxConnections);
        ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
      }
    }
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    connections.dec();

    if (maxConnections > 0) {
      if (numConnections.decrementAndGet() < 0) {
        log.error("BUG in ConnectionLimiter");
      }
    }
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }
}
