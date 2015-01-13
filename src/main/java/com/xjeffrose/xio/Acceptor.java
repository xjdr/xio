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

  Acceptor(ServerSocketChannel serverChannel) {
    this.serverChannel = serverChannel;

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
            log.info("Accepting Connection from: " + channel);
            channel.configureBlocking(false);
            ChannelContext ctx = new ChannelContext(channel);
            channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, ctx);
          }

          if (key.isReadable()) {
              SocketChannel client = (SocketChannel) key.channel();
              ChannelContext ctx = (ChannelContext) key.attachment();
              ctx.read();
          }

          if (key.isWritable()) {
              SocketChannel client = (SocketChannel) key.channel();
              ChannelContext ctx = (ChannelContext) key.attachment();
              ctx.write();
          }

        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      }
    }
  }
}
