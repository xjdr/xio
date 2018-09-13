package com.xjeffrose.xio.firewall;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.hash.Funnels;
import com.google.common.util.concurrent.RateLimiter;
import com.xjeffrose.xio.core.Constants;
import com.xjeffrose.xio.server.RendezvousHash;
import com.xjeffrose.xio.server.ServerLimits;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class ServiceRateLimiter extends ChannelDuplexHandler {
  private final Map<CharSequence, RateLimiter> softLimiterMap =
      PlatformDependent.newConcurrentHashMap();
  private final Map<CharSequence, RateLimiter> hardLimiterMap =
      PlatformDependent.newConcurrentHashMap();
  private final Meter reqs;
  private final Timer timer;
  private final ServerLimits config;
  private final RendezvousHash<CharSequence> softRateLimitHasher;
  private final RendezvousHash<CharSequence> hardRateLimitHasher;
  private final RateLimiter globalHardLimiter;
  private final RateLimiter globalSoftLimiter;

  private Map<ChannelHandlerContext, Timer.Context> timerMap =
      PlatformDependent.newConcurrentHashMap();

  public ServiceRateLimiter(MetricRegistry metrics, ServerLimits config) {
    this.reqs = metrics.meter(name("requests", "Rate"));
    this.timer = metrics.timer("Request Latency");
    this.config = config;
    this.globalHardLimiter = RateLimiter.create(config.globalHardReqPerSec());
    this.globalSoftLimiter = RateLimiter.create(config.globalSoftReqPerSec());

    softRateLimitHasher =
        buildHasher(softLimiterMap, config.rateLimiterPoolSize(), config.softReqPerSec());
    hardRateLimitHasher =
        buildHasher(hardLimiterMap, config.rateLimiterPoolSize(), config.hardReqPerSec());
  }

  private RendezvousHash<CharSequence> buildHasher(
      Map<CharSequence, RateLimiter> limiterMap, int poolSize, double rate) {
    List<String> tempPool = new ArrayList<>();

    for (int i = 0; i < poolSize; i++) {
      String id = UUID.randomUUID().toString();
      tempPool.add(id);
      limiterMap.put(id, RateLimiter.create(rate));
    }

    return new RendezvousHash<>(Funnels.stringFunnel(Constants.DEFAULT_CHARSET), tempPool);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    reqs.mark();

    // Rate Limit per server
    String remoteAddress =
        ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

    if (config.clientRateLimitOverride().containsKey(remoteAddress)) {
      if (hardLimiterMap.containsKey(remoteAddress)) {
        if (!hardLimiterMap.get(remoteAddress).tryAcquire()) {
          log.debug("Hard Rate limit fired for {}", remoteAddress);
          ctx.channel().attr(Constants.HARD_RATE_LIMITED).set(Boolean.TRUE);
        } else if (!softLimiterMap.get(remoteAddress).tryAcquire()) {
          ctx.channel().attr(Constants.SOFT_RATE_LIMITED).set(Boolean.TRUE);
        }
      } else {
        hardLimiterMap.put(
            remoteAddress,
            RateLimiter.create(config.clientRateLimitOverride().get(remoteAddress).get(1)));
        softLimiterMap.put(
            remoteAddress,
            RateLimiter.create(config.clientRateLimitOverride().get(remoteAddress).get(0)));
      }

    } else {
      if (!hardLimiterMap
          .get(hardRateLimitHasher.getOne(remoteAddress.getBytes(Constants.DEFAULT_CHARSET)))
          .tryAcquire()) {
        log.debug("Hard Rate limit fired for " + remoteAddress);
        ctx.channel().attr(Constants.HARD_RATE_LIMITED).set(Boolean.TRUE);
      } else if (!softLimiterMap
          .get(softRateLimitHasher.getOne(remoteAddress.getBytes(Constants.DEFAULT_CHARSET)))
          .tryAcquire()) {
        ctx.channel().attr(Constants.SOFT_RATE_LIMITED).set(Boolean.TRUE);
      }
    }

    // Global Rate Limiter
    if (!globalHardLimiter.tryAcquire()) {
      log.debug("Global Hard Rate limit fired");
      ctx.channel().attr(Constants.HARD_RATE_LIMITED).set(Boolean.TRUE);

    } else if (!globalSoftLimiter.tryAcquire()) {
      ctx.channel().attr(Constants.SOFT_RATE_LIMITED).set(Boolean.TRUE);
    }

    ctx.fireChannelActive();

    timerMap.put(ctx, timer.time());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelInactive();

    timerMap.remove(ctx).stop();
  }
}
