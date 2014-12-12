package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;

class Acceptor {
  private static final Logger log = com.xjeffrose.log.Log.create();

  private final ServerSocketChannel channel;
  private final ServerSocket socket;
  private final InetSocketAddress address;

  Acceptor(int port) throws IOException {
    channel = ServerSocketChannel.open();
    socket = channel.socket();
    address = new InetSocketAddress(port);
  }

  public void registerWithSelector(Selector selector, Acceptable attachment) throws IOException {
    socket.bind(address);
    channel.configureBlocking(false);
    channel.register(selector, SelectionKey.OP_ACCEPT, attachment);
  }

  Connection accept() throws IOException {
    return new Connection(channel.accept());
  }

}
