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

  class EventLoop extends Thread {
    private final Logger log = Log.getLogger(EventLoop.class.getName());
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final Selector selector;
    private final ConcurrentLinkedDeque<SocketChannel> channelsToAdd = new ConcurrentLinkedDeque<SocketChannel>();

    EventLoop() {
      try {
        selector = Selector.open();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

    }

    public void addChannel(SocketChannel channel) {
      channelsToAdd.push(channel);
      selector.wakeup();
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

        Set<SelectionKey> cancelMeKeys = new HashSet<SelectionKey>();

        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          iterator.remove();

          try {
            if (key.isValid() && key.isReadable()) {
              SocketChannel client = (SocketChannel) key.channel();
              ChannelContext ctx = (ChannelContext) key.attachment();
              ctx.read();
            }

            if (key.isValid() && key.isWritable()) {
              SocketChannel client = (SocketChannel) key.channel();
              ChannelContext ctx = (ChannelContext) key.attachment();
              if (ctx.write()) {
                cancelMeKeys.add(key);
              }
            }

          } catch (Exception e) {
            throw new RuntimeException(e);
          }

        }

        while (channelsToAdd.size() > 0) {
          try {
            SocketChannel channel = channelsToAdd.pop();
            channel.configureBlocking(false);
            ChannelContext ctx = new ChannelContext(channel);
            channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, ctx);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  private final ConcurrentLinkedDeque<EventLoop> pool = new ConcurrentLinkedDeque<EventLoop>();

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
    EventLoop loop = pool.removeFirst();
    pool.addLast(loop);
    return loop;
  }
}
