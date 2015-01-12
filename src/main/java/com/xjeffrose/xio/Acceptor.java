package com.xjeffrose.xio;

import java.io.*;
import java.nio.channels.*;
/* import java.util.concurrent.*; */
import java.util.concurrent.atomic.*;
import java.util.logging.*;
import java.util.stream.*;

import com.xjeffrose.log.*;

class Acceptor extends Thread {
  private static final Logger log = Log.getLogger(Acceptor.class.getName());

  private final AtomicBoolean isRunning = new AtomicBoolean(true);
  private final AtomicBoolean isReady = new AtomicBoolean(true);
  private final Gatekeeper g = new Gatekeeper();
  private IOService[] ioPool;

  private Selector selector;

  Acceptor() {
    try {
      selector = Selector.open();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean Ready() {
    return isReady.get();
  }

  public boolean Running() {
    return isRunning.get();
  }

  public void register(ServerSocketChannel channel) {
    try {
      channel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void ioPool(IOService[] ioPool) {
    this.ioPool = ioPool;
  }

  private void doAccept(SelectionKey key) {
    //TODO: Clean up this Logic
    g.ioPool(ioPool);
    log.info("ohai");
    g.accept(key);
    g.ipFilter();
    g.rateLimit();
  }

  private void process() {
    while (Running() && Ready()) {
      try {
        Thread.sleep(12);
        selector.select();
        selector.selectedKeys()
            .stream()
            .distinct()
            .filter(SelectionKey::isAcceptable)
            .forEach(this::doAccept);
      } catch (Exception e) {
        log.info(""+e);
        throw new RuntimeException(e);
      }
    }
  }


  public void close() {
    isRunning.set(false);
  }

  public void run() {
    process();
  }

}
