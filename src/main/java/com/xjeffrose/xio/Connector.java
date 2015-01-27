package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Connector {
  private static final Logger log = Log.getLogger(Connector.class.getName());

  private final AtomicBoolean isRunning = new AtomicBoolean(true);
  private final AtomicBoolean isReady = new AtomicBoolean(true);
  private final SocketChannel clientChannel;
  private final Selector selector;
  private final EventLoopPool eventLoopPool;
  private final ClientChannelContext context;

  Connector(SocketChannel clientChannel, EventLoopPool eventLoopPool) {
    this.clientChannel = clientChannel;
    this.eventLoopPool = eventLoopPool;
    context = new ClientChannelContext(clientChannel);

    try {
    selector = Selector.open();
    clientChannel.register(selector, SelectionKey.OP_CONNECT);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Future<HttpResponse> getResponse() {
    return context.promise;
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

  public void start() { run(); }

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
            next.addContext(context);
            isRunning.set(false);
          } else if (!key.isValid() || !key.isConnectable()) {
            key.cancel();
            isRunning.set(false);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
