package com.xjeffrose.xio;

import java.io.*;
import java.nio.channels.*;
import java.util.logging.*;

class Connection implements Closeable, Event, Readable, Writable {
  private static final Logger log = com.xjeffrose.log.Log.create();

  private final SocketChannel channel;
  private Delegate delegate;

  interface Delegate {
    void read(SocketChannel channel);
    void write(SocketChannel channel);
  }

  Connection(SocketChannel channel) {
    this.channel = channel;
  }

  void setDelegate(Delegate delegate) {
    this.delegate = delegate;
  }

  @Override public void doRead() throws IOException {
    if (delegate != null) {
      delegate.read(channel);
    }
  }

  @Override public void doWrite() throws IOException {
    if (delegate != null) {
      delegate.write(channel);
    }
  }

  @Override public void close() throws IOException {
  }

  @Override public void registerWithEventLoop(EventLoop loop) throws IOException {
    log.info("registering with EventLoop " + this);
    channel.configureBlocking(false);
    channel.register(loop.getSelector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
  }

}
