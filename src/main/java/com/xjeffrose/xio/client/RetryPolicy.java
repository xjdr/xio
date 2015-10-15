package com.xjeffrose.xio.client;

public interface RetryPolicy {
  boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper);
}
