package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Connector extends Thread {
  private static final Logger log = Log.getLogger(Connector.class.getName());

  private final AtomicBoolean isRunning = new AtomicBoolean(true);
  private final AtomicBoolean isReady = new AtomicBoolean(true);
  private final SocketChannel clientChannel;
  private final Selector selector;
  private final EventLoopPool eventLoopPool;

  Connector(SocketChannel clientChannel, EventLoopPool eventLoopPool) {
    this.clientChannel = clientChannel;
    this.eventLoopPool = eventLoopPool;

    try {
    selector = Selector.open();
    clientChannel.register(selector, SelectionKey.OP_CONNECT);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean ready() {
    return isReady.get();
  }

  public boolean running() {
    return isRunning.get();
  }

  public void close() {
    isRunning.set(false);
  }

  public void run() {
    while(running()){
      try {
        selector.select();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      Set<SelectionKey> connectKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = connectKeys.iterator();

      while (iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();

        try {
          if (key.isValid() && key.isConnectable()) {
            SocketChannel channel = (SocketChannel) key.channel();
            if (!channel.finishConnect()) {
              key.cancel();
              throw new RuntimeException();
            }
            //log.info("Connecting to: " + channel);
            EventLoop next = eventLoopPool.next();
            next.addChannel(channel);
          } else if (!key.isValid() || !key.isConnectable()) {
            key.cancel();
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
