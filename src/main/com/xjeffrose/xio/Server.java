package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
//import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

class Server {
  private final int port;

  Server(int port) throws IOException {
    this.port = port;

    ServerSocketChannel channel = ServerSocketChannel.open();
    channel.bind(new InetSocketAddress(8080));
    channel.configureBlocking(false);

    Selector selector = Selector.open();
    SelectionKey key = channel.register(selector, SelectionKey.OP_ACCEPT);

  }

}
