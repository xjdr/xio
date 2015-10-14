package com.xjeffrose.xio.client.retry;


import com.xjeffrose.xio.client.RetryPolicy;
import com.xjeffrose.xio.client.RetrySleeper;
import java.util.concurrent.TimeUnit;

abstract class SleepingRetry implements RetryPolicy {
  private final int n;

  protected SleepingRetry(int n) {
    this.n = n;
  }

  // made public for testing
  public int getN() {
    return n;
  }

  public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper) {
    if (retryCount < n) {
      try {
        sleeper.sleepFor(getSleepTimeMs(retryCount, elapsedTimeMs), TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
      return true;
    }
    return false;
  }

  protected abstract int getSleepTimeMs(int retryCount, long elapsedTimeMs);
}
