package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
/* import java.util.*; */
import java.util.logging.*;

import com.xjeffrose.log.*;

class Server {
  private static final Logger log = Log.getLogger(Server.class.getName());

  private final int port;
  private final InetSocketAddress addr;
  private final ServerSocket socket;
  private final ServerSocketChannel channel;
  private final EventLoop ev;

  Server(int port) throws IOException {
    this.port = port;

    ev = new EventLoop();
    addr = new InetSocketAddress(port);
    channel = ServerSocketChannel.open();
    socket = channel.socket();
    configureNetworking();
  }

  private void configureNetworking() throws IOException {
    channel.configureBlocking(false);
    socket.bind(addr);
    ev.register(channel);
  }

  /* private void announce(String path, Set<String> zkHosts) { */
  /* } */

  private void addRoute(String route, Service service) {
  }

  void serve() throws IOException {
    ev.run();
  }

}
