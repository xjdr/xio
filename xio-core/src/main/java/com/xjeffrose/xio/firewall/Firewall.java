package com.xjeffrose.xio.firewall;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.xjeffrose.xio.core.Constants;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Slf4j
@ChannelHandler.Sharable
public class Firewall extends ChannelDuplexHandler {

  private final Meter rateLimits;

  public Firewall(MetricRegistry metrics) {
    this.rateLimits = metrics.meter("Hard Rate Limits");
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    if (ctx.channel().hasAttr(Constants.HARD_RATE_LIMITED)) {
      if (log.isErrorEnabled()) {
        log.error("Channel {} Closed due to Hard Rate Limit being reached", ctx.channel());
      }

      rateLimits.mark();
      ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
    }

    if ((ctx.channel().hasAttr(Constants.IP_BLACK_LIST))) {
      if (log.isErrorEnabled()) {
        log.error("Channel {} Closed due to IP Black List Configuration", ctx.channel());
      }
      ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
    }

    // This will always be set to False
    if ((ctx.channel().hasAttr(Constants.IP_WHITE_LIST))) {
      if (log.isErrorEnabled()) {
        log.error("{} is not a white listed client. Dropping Connection", ctx.channel());
      }
      ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
    }

    ctx.fireChannelActive();
  }

  @VisibleForTesting
  public Logger getLog() {
    return log;
  }
}
