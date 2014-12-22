package com.xjeffrose.util;

class Await {

  Await() {}

  public boolean ready(Future<?> f) {
    return true;
  }

  public boolean ready(Future<?> f, String timeOut) {
    return true;
  }

  public <T> T result(Future<T> f) {
    return f.collect();
  }

}
