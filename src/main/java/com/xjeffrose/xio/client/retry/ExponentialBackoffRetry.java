package com.xjeffrose.xio.client.retry;

import java.util.Random;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExponentialBackoffRetry extends SleepingRetry {


  private static final int MAX_RETRIES_LIMIT = 29;
  private static final int DEFAULT_MAX_SLEEP_MS = Integer.MAX_VALUE;

  private final Random random = new Random();
  private final int baseSleepTimeMs;
  private final int maxSleepMs;

  /**
   * @param baseSleepTimeMs initial amount of time to wait between retries
   * @param maxRetries max number of times to retry
   */
  public ExponentialBackoffRetry(int baseSleepTimeMs, int maxRetries) {
    this(baseSleepTimeMs, maxRetries, DEFAULT_MAX_SLEEP_MS);
  }

  /**
   * @param baseSleepTimeMs initial amount of time to wait between retries
   * @param maxRetries max number of times to retry
   * @param maxSleepMs max time in ms to sleep on each retry
   */
  public ExponentialBackoffRetry(int baseSleepTimeMs, int maxRetries, int maxSleepMs) {
    super(validateMaxRetries(maxRetries));
    this.baseSleepTimeMs = baseSleepTimeMs;
    this.maxSleepMs = maxSleepMs;
  }

  private static int validateMaxRetries(int maxRetries) {
    if (maxRetries > MAX_RETRIES_LIMIT) {
      log.warn(String.format("maxRetries too large (%d). Pinning to %d", maxRetries, MAX_RETRIES_LIMIT));
      maxRetries = MAX_RETRIES_LIMIT;
    }
    return maxRetries;
  }

  public int getBaseSleepTimeMs() {
    return baseSleepTimeMs;
  }

  @Override
  protected int getSleepTimeMs(int retryCount, long elapsedTimeMs) {
    // copied from Hadoop's RetryPolicies.java
    int sleepMs = baseSleepTimeMs * Math.max(1, random.nextInt(1 << (retryCount + 1)));
    if (sleepMs > maxSleepMs) {
      log.warn(String.format("Sleep extension too large (%d). Pinning to %d", sleepMs, maxSleepMs));
      sleepMs = maxSleepMs;
    }
    return sleepMs;
  }
}
