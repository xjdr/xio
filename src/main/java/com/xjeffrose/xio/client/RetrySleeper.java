package com.xjeffrose.xio.client;

import java.util.concurrent.TimeUnit;

public interface RetrySleeper {
  void sleepFor(long time, TimeUnit unit) throws InterruptedException;
}
