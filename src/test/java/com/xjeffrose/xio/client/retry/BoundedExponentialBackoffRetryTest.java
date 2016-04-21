package com.xjeffrose.xio.client.retry;

import com.xjeffrose.xio.core.XioTransportException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

import static org.junit.Assert.*;

public class BoundedExponentialBackoffRetryTest {
  BoundedExponentialBackoffRetry retry = new BoundedExponentialBackoffRetry(500, 5000, 4);
  int retries = 0;

  @Test
  public void getMaxSleepTimeMs() throws Exception {
    assertEquals(5000, retry.getMaxSleepTimeMs());
  }

  @Test(expected = ConnectException.class)
  public void getSleepTimeMs() throws Exception {
    TracerDriver tracerDriver = new TracerDriver() {

      @Override
      public void addTrace(String name, long time, TimeUnit unit) {
      }

      @Override
      public void addCount(String name, int increment) {
      }
    };

    RetryLoop rt_loop = new RetryLoop(retry, new AtomicReference<>(tracerDriver));

    for (int i = 0; i < 5; i++) {
      rt_loop.takeException(new ConnectException("connection failure"));
    }
  }
}