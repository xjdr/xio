package com.xjeffrose.xio.client.asyncretry;

import io.netty.channel.EventLoopGroup;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsyncRetryLoop {
  private final int attemptLimit;
  private final EventLoopGroup eventLoopGroup;
  private final long delay;
  private final TimeUnit unit;
  private final AtomicInteger attemptCount = new AtomicInteger(0);

  public AsyncRetryLoop(int attemptLimit, EventLoopGroup eventLoopGroup, long delay,
    TimeUnit unit) {
    this.attemptLimit = attemptLimit;
    this.eventLoopGroup = eventLoopGroup;
    this.delay = delay;
    this.unit = unit;
  }

  public void attempt(Runnable action) {
    if (attemptCount.incrementAndGet() == 1) {
      action.run();
    } else {
      eventLoopGroup.schedule(action, delay, unit);
    }
  }

  public boolean canRetry() {
    return attemptCount.get() < attemptLimit;
  }
}
