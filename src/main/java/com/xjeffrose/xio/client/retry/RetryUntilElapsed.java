package com.xjeffrose.xio.client.retry;

import com.xjeffrose.xio.client.RetrySleeper;

public class RetryUntilElapsed extends SleepingRetry {
  private final int maxElapsedTimeMs;
  private final int sleepMsBetweenRetries;

  public RetryUntilElapsed(int maxElapsedTimeMs, int sleepMsBetweenRetries) {
    super(Integer.MAX_VALUE);
    this.maxElapsedTimeMs = maxElapsedTimeMs;
    this.sleepMsBetweenRetries = sleepMsBetweenRetries;
  }

  @Override
  public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) {
    return super.allowRetry(retryCount, elapsedTimeMs, sleeper) && (elapsedTimeMs < maxElapsedTimeMs);
  }

  @Override
  protected int getSleepTimeMs(int retryCount, long elapsedTimeMs) {
    return sleepMsBetweenRetries;
  }
}

