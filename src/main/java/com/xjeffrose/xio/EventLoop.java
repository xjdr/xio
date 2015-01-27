package com.xjeffrose.xio;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class EventLoop extends Thread {
  private final Logger log = Log.getLogger(EventLoop.class.getName());

  private final ConcurrentLinkedDeque<SocketChannel> channelsToAdd = new ConcurrentLinkedDeque<SocketChannel>();
  private final ConcurrentLinkedDeque<ChannelContext> contextsToAdd = new ConcurrentLinkedDeque<ChannelContext>();
  private final AtomicBoolean isRunning = new AtomicBoolean(true);
  private final Selector selector;

  private Map<String, Service> routes;

  EventLoop() {
    try {
      selector = Selector.open();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void addRoutes(Map<String, Service> routes) {
    this.routes = routes;
  }

  public void addChannel(SocketChannel channel) {
    channelsToAdd.push(channel);
    selector.wakeup();
  }

  public void addContext(ChannelContext context) {
    contextsToAdd.push(context);
    /* selector.wakeup(); */
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

      Set<SelectionKey> acceptKeys = selector.selectedKeys();
      Iterator<SelectionKey> iterator = acceptKeys.iterator();

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
            ctx.write();
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      _addChannels();
      /* _addContexts(); */
    }
  }

  private void _addChannels() {
    while (channelsToAdd.size() > 0) {
      try {
        SocketChannel channel = channelsToAdd.pop();
        channel.configureBlocking(false);
        ChannelContext ctx = new ChannelContext(channel, routes);
        channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, ctx);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void _addContexts() {
    while (contextsToAdd.size() > 0) {
      try {
        SocketChannel channel = channelsToAdd.pop();
        ChannelContext context = contextsToAdd.pop();
        context.channel.configureBlocking(false);
        context.channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, context);
        //TODO: context.channel.register(selector, context.interestedOps(), context);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}

