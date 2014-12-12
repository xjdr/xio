package com.xjeffrose.xio;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;
import java.util.concurrent.atomic.*;

class EventLoop implements Closeable {
  private static final Logger log = com.xjeffrose.log.Log.create();

  private final Selector selector;
  private final AtomicBoolean running = new AtomicBoolean(true);

  EventLoop() throws IOException {
    selector = Selector.open();
  }

  void register(Event event) throws IOException {
    event.registerWithEventLoop(this);
  }

  @Override public void close() throws IOException {
    running.set(false);
  }

  void run() throws IOException {
    while (running.get()) {
      selector.select();
      Stream<SelectionKey> keyStream = selector.selectedKeys().stream();

      keyStream
        .forEach(key -> {
        try {
          if (key.isAcceptable()) {
            Acceptable attachment = (Acceptable) key.attachment();
            attachment.doAccept();
          }
          if (key.isReadable()) {
            Readable attachment = (Readable) key.attachment();
            attachment.doRead();
          }
          if (key.isWritable()) {
            Writable attachment = (Writable) key.attachment();
            attachment.doWrite();
          }

        } catch (IOException e) {
          e.printStackTrace();
        }
      });

      // VERY IMPORTANT YET SUBTLE LINE OF CODE!
      selector.selectedKeys().clear();
    }
  }

  Selector getSelector() {
    return selector;
  }

}
