package com.xjeffrose.xio.client.retry;

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

  @Test(expected = Exception.class)
  public void getSleepTimeMs() throws Exception {
//    assertEquals(500, retry.getSleepTimeMs(0, 200));
//    assertEquals(1000, retry.getSleepTimeMs(1, 400));
//    assertEquals(3000, retry.getSleepTimeMs(2, 600));
//    assertEquals(3500, retry.getSleepTimeMs(3, 800));
//    assertEquals(600, retry.getSleepTimeMs(4, 1000));
//    assertEquals(700, retry.getSleepTimeMs(5, 1300));
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
//      System.out.println(rt_loop.);
      System.out.println(retry.getSleepTimeMs(i, 200));
      System.out.print(retry.allowRetry(i, 200, new RetrySleeper() {
        @Override
        public void sleepFor(long time, TimeUnit unit) throws InterruptedException {

        }
      }));
      rt_loop.takeException(new Exception());
    }

  }

}