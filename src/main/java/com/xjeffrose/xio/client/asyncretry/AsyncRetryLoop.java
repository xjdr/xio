package com.xjeffrose.xio.client.asyncretry;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ConnectTimeoutException;
import java.net.ConnectException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.log4j.Log4j;

@Log4j
public class AsyncRetryLoop {
  private final int attemptLimit;
  private final EventLoopGroup eventLoopGroup;
  private final long delay;
  private final TimeUnit unit;
  private int attemptCount = 0;

  public AsyncRetryLoop(int attemptLimit, EventLoopGroup eventLoopGroup, long delay, TimeUnit unit) {
    this.attemptLimit = attemptLimit;
    this.eventLoopGroup = eventLoopGroup;
    this.delay = delay;
    this.unit = unit;
  }

  public void attempt(Runnable action) {
    attemptCount++;
    if (attemptCount == 1) {
      action.run();
    } else {
      eventLoopGroup.schedule(action, delay, unit);
    }
  }

  public boolean canRetry() {
    return attemptCount < attemptLimit;
  }
}
