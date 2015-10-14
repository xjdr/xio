package com.xjeffrose.xio.client;

public interface RetryPolicy {
  public boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper);
}
