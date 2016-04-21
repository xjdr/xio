package com.xjeffrose.xio.client.retry;

public interface RetryPolicy {
  boolean allowRetry(int retryCount, long elapsedTimeMs, RetrySleeper sleeper);
}
