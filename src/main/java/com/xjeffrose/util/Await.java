package com.xjeffrose.util;

import java.io.*;
import java.time.*;

class Await {

  Await() {}

  // WARNING: This call will not timeout... Ever ...
  public <T> T ready(Future<T> f) {
    while (f.isReady.get() == false) {
      if (f.isError.get()) {
        break;
      }
    }
    return f.collect();
  }

  // This is the method you should really be using unless you have
  // a really, really good reason
  public <T> T ready(Future<T> f, long timeout) {
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
  public <T> T result(Future<T> f) {
    return f.collect();
  }

  public <T> boolean isReady(Future<T> f) {
    return f.isReady.get();
  }

}
