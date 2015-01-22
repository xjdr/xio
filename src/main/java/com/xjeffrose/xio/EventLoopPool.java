package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class EventLoopPool {
  private static final Logger log = Log.getLogger(EventLoopPool.class.getName());

  private final ConcurrentLinkedDeque<EventLoop> pool = new ConcurrentLinkedDeque<EventLoop>();
  private EventLoop loop;

  EventLoopPool(int poolSize) {
    for (int i=0; i<poolSize; i++) {
      pool.addLast(new EventLoop());
    }
  }

  public void start() {
    for(EventLoop loop : pool) {
      loop.start();
    }
  }

  public EventLoop next() {
    loop = pool.removeFirst();
    pool.addLast(loop);
    return loop;
  }
}
