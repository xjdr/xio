package com.xjeffrose.xio.client.retry;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class RetryForever implements RetryPolicy {


  private final int retryIntervalMs;

  public RetryForever(int retryIntervalMs) {
    checkArgument(retryIntervalMs > 0);
    this.retryIntervalMs = retryIntervalMs;
  }

  @Override
  public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) {
    try {
      sleeper.sleepFor(retryIntervalMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Error occurred while sleeping", e);
      return false;
    }
    return true;
  }
}
