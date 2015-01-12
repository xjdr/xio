package com.xjeffrose.xio;

import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;
import java.util.stream.*;

import com.xjeffrose.log.*;

class IOService extends Thread {
  private static final Logger log = Log.getLogger(IOService.class.getName());

  private final AtomicBoolean isRunning = new AtomicBoolean(true);
  private final AtomicBoolean isReady = new AtomicBoolean(true);

  private Selector ioSelector;

  IOService() {
    try {
      ioSelector = Selector.open();
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

  public void register(SocketChannel channel) {
    log.info("Registered " + channel);
    try {
      channel.configureBlocking(false);
      channel.register(ioSelector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void readEvent(SelectionKey key) {
    log.info("am reading " + key);
  }

  private void writeEvent(SelectionKey key) {
  }

  private void process() {
    try{
      while (Ready()) {
        Thread.sleep(1200);
        ioSelector.select();
        log.info("IOServ");
        ioSelector.selectedKeys()
            .stream()
            .distinct()
            /* .filter(SelectionKey::isReadable) */
            .forEach(this::readEvent);
      }
    } catch (Exception e) {
      log.info(""+e);
      throw new RuntimeException(e);
    }
  }


  public void close() {
    isRunning.set(false);
  }

  public void run() {
    while (Running()) {
      process();
    }
  }

}
