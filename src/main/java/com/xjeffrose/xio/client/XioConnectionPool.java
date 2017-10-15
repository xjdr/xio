package com.xjeffrose.xio.client;

import com.google.common.base.Preconditions;
import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoop;
import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoopFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Opens and maintains some number of connections to a given endpoint.
 *
 * At least 1 valid connection will be available at any point in time
 * assuming that the network hasn't been nuked.
 *
 * Retry logic will be used to establish a valid connection in a
 * turbulent network.
 */
public class XioConnectionPool {
  private final EventLoopGroup eventLoopGroup;
  private final AsyncRetryLoopFactory retryLoopFactory;
  private final ChannelHandler handler;
  private final SimpleChannelPool simpleChannelPool;
  private final AtomicInteger channelCount = new AtomicInteger(0);
  private final AtomicInteger acquiredCount = new AtomicInteger(0);
  private final AtomicInteger releasedCount = new AtomicInteger(0);
  private final AtomicInteger passedHealthCheckCount = new AtomicInteger(0);
  private final AtomicInteger failedHealthCheckCount = new AtomicInteger(0);
  private final ChannelPoolHandler channelPoolHandler = new ChannelPoolHandler() {
    @Override
    public void channelCreated(Channel ch) {
      channelCount.incrementAndGet();
      ch.pipeline().addLast(handler);
    }

    @Override
    public void channelReleased(Channel ch) {
      releasedCount.incrementAndGet();
    }

    @Override
    public void channelAcquired(Channel ch) {
      acquiredCount.incrementAndGet();
    }
  };
  private final ChannelHealthChecker channelHealthChecker = new ChannelHealthChecker() {
    @Override
    public Future<Boolean> isHealthy(Channel channel) {
      EventLoop loop = channel.eventLoop();
      if (channel.isActive()) {
        passedHealthCheckCount.incrementAndGet();
        return loop.newSucceededFuture(Boolean.TRUE);
      } else {
        failedHealthCheckCount.incrementAndGet();
        return loop.newSucceededFuture(Boolean.FALSE);
      }
    }
  };

  public XioConnectionPool(Bootstrap bootstrap, AsyncRetryLoopFactory retryLoopFactory) {
    Preconditions.checkNotNull(bootstrap);
    this.retryLoopFactory = Preconditions.checkNotNull(retryLoopFactory);
    handler = bootstrap.config().handler();
    eventLoopGroup = bootstrap.config().group();
    simpleChannelPool = new SimpleChannelPool(bootstrap, channelPoolHandler, channelHealthChecker);
  }

  private void acquireWithRetry(AsyncRetryLoop retry, DefaultPromise<Channel> result) {
    Future<Channel> poolResult = simpleChannelPool.acquire();
    poolResult.addListener(new FutureListener<Channel>() {
      public void operationComplete(Future<Channel> f) {
        if (f.isSuccess()) {
          result.setSuccess(f.getNow());
        } else {
          // deal with connection failure here.
          if (retry.canRetry()) {
            retry.attempt(() -> acquireWithRetry(retry, result));
          } else {
            result.setFailure(f.cause());
          }
        }
      }
    });
  }

  public Future<Channel> acquire() {
    final DefaultPromise<Channel> result = new DefaultPromise<>(eventLoopGroup.next());
    final AsyncRetryLoop retry = retryLoopFactory.buildLoop(eventLoopGroup);
    retry.attempt(() -> acquireWithRetry(retry, result));
    return result;
  }

  public Future<Void> release(Channel ch) {
    return simpleChannelPool.release(ch);
  }
}
