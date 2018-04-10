package com.xjeffrose.xio.client.retry;

public class RetryNTimes extends SleepingRetry {
  private final int sleepMsBetweenRetries;

  public RetryNTimes(int n, int sleepMsBetweenRetries) {
    super(n);
    this.sleepMsBetweenRetries = sleepMsBetweenRetries;
  }

  @Override
  protected int getSleepTimeMs(int retryCount, long elapsedTimeMs) {
    return sleepMsBetweenRetries;
  }
}
