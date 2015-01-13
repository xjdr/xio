package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Acceptor extends Thread {
  private static final Logger log = Log.getLogger(Acceptor.class.getName());

  private final AtomicBoolean isRunning = new AtomicBoolean(true);
  private final AtomicBoolean isReady = new AtomicBoolean(true);
  private final ServerSocketChannel serverChannel;
  private final Selector selector;
  private final EventLoopPool eventLoopPool;

  Acceptor(ServerSocketChannel serverChannel, EventLoopPool eventLoopPool) {
    this.serverChannel = serverChannel;
    this.eventLoopPool = eventLoopPool;

    try {
    selector = Selector.open();
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  public boolean Ready() {
    return isReady.get();
  }

  public boolean Running() {
    return isRunning.get();
  }

  public void close() {
    isRunning.set(false);
  }

  public void run() {
    while(Running()){
      try {
        selector.select();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      Set<SelectionKey> acceptKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = acceptKeys.iterator();

      while (iterator.hasNext()) {
        SelectionKey key = iterator.next();
        iterator.remove();

        try {
          if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel channel = server.accept();
            //log.info("Accepting Connection from: " + channel);
            EventLoopPool.EventLoop next = eventLoopPool.next();
            next.addChannel(channel);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      }
    }
  }
}
