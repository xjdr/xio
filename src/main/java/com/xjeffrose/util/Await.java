package com.xjeffrose.util;

import java.io.*;
import java.time.*;

public class Await {

  Await() {}

  // WARNING: This call will not timeout... Ever ...
  public static <T extends Runnable> T ready(Future<T> f) {
    while (f.isReady.get() == false) {
      if (f.isError.get()) {
        break;
      }
    }
    return f.collect();
  }

  // This is the method you should really be using unless you have
  // a really, really good reason
  public static <T extends Runnable> T ready(Future<T> f, long timeout) {
    LocalTime gameOver = LocalTime.now().plusSeconds(timeout);

    while (f.isReady.get() == false) {

      if (f.isError.get()) {
        break;
      }

      if (LocalTime.now().isAfter(gameOver)) {
        //Throw timeout
        break;
      }
    }

    return f.collect();
  }

  // WARNING: this call is blocking!!
  public <T extends Runnable> T result(Future<T> f) {
    return f.collect();
  }

  public <T extends Runnable> boolean isReady(Future<T> f) {
    return f.isReady.get();
  }

}
