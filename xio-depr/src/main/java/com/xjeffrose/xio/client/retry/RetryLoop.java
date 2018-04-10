package com.xjeffrose.xio.client.retry;

import io.netty.channel.ConnectTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryLoop {
  private static final RetrySleeper sleeper =
      new RetrySleeper() {
        @Override
        public void sleepFor(long time, TimeUnit unit) throws InterruptedException {
          unit.sleep(time);
        }
      };

  private final long startTimeMs = System.currentTimeMillis();
  private final RetryPolicy retryPolicy;
  private final AtomicReference<TracerDriver> tracer;
  private boolean isDone = false;
  private int retryCount = 0;

  public RetryLoop(RetryPolicy retryPolicy, AtomicReference<TracerDriver> tracer) {
    this.retryPolicy = retryPolicy;
    this.tracer = tracer;
  }

  public static RetrySleeper getDefaultRetrySleeper() {
    return sleeper;
  }

  public static boolean shouldRetry(int rc) {
    return true;
  }

  public static boolean isRetryException(Throwable exception) {
    if (exception instanceof ConnectException
        || exception instanceof ConnectTimeoutException
        || exception instanceof UnknownHostException) {
      return true;
    }
    return false;
  }

  public boolean shouldContinue() {
    return !isDone;
  }

  public void markComplete() {
    isDone = true;
  }

  public void takeException(Exception exception) throws Exception {
    boolean rethrow = true;
    if (isRetryException(exception)) {

      if (retryPolicy.allowRetry(retryCount++, System.currentTimeMillis() - startTimeMs, sleeper)) {
        tracer.get().addCount("retries-allowed", 1);
        rethrow = false;
      } else {
        tracer.get().addCount("retries-disallowed", 1);
      }
    }

    if (rethrow) {
      throw exception;
    }
  }
}
