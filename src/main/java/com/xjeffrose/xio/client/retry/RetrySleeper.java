package com.xjeffrose.xio.client.retry;

import java.util.concurrent.TimeUnit;

public interface RetrySleeper {
  void sleepFor(long time, TimeUnit unit) throws InterruptedException;
}
