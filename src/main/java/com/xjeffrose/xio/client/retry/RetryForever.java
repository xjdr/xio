package com.xjeffrose.xio.client.retry;

import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

import static com.google.common.base.Preconditions.checkArgument;

public class RetryForever implements RetryPolicy {
  private static final Logger log = Logger.getLogger(RetryForever.class);

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
