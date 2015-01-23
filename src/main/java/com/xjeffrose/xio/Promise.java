package com.xjeffrose.xio;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Promise<V> implements Future<V> {
  private static final Logger log = Log.getLogger(Promise.class.getName());

  private AtomicReference<V> value = new AtomicReference<V>();

  Promise() {
  }

  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  public V get() {
    synchronized (value) {
      while(!isDone()) {
        try {
          value.wait();
        } catch(InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return value.get();
  }

  public V get(long timeout, TimeUnit unit) {
    throw new RuntimeException("not implemented");
  }

  public boolean isCancelled() {
    return false;
  }

  public boolean isDone() {
    return value.get() != null;
  }

  public void set(V value) {
    synchronized (this.value) {
      this.value.set(value);
      this.value.notifyAll();
    }
  }
}
