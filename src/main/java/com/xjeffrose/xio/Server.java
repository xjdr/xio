package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Server {
  private static final Logger log = Log.getLogger(Server.class.getName());

  private final Map<String, Service> routes = new ConcurrentHashMap<String, Service>();

  private int port;
  private int cores;
  private InetAddress host;
  private InetSocketAddress addr;
  private ServerSocketChannel channel;

  Server() {
  }

  private void schedule(ServerSocketChannel channel) {
    cores = Runtime.getRuntime().availableProcessors();
    EventLoopPool pool = new EventLoopPool(cores);
    Acceptor acceptor = new Acceptor(channel, pool, routes);
    acceptor.start();
    pool.start();
  }

  private void bind(InetSocketAddress addr) throws IOException {
    channel = ServerSocketChannel.open();
    channel.configureBlocking(false);
    channel.bind(addr);
    channel.socket().setReuseAddress(true);
    schedule(channel);
  }

  /* private void announce(String path, Set<String> zkHosts) { */
  /* } */

  void addRoute(String route, Service service) {
    routes.putIfAbsent(route, service);
  }

  void serve(int port) throws IOException {
    cores = Runtime.getRuntime().availableProcessors();
    addr = new InetSocketAddress(port);
    bind(addr);
  }

  void serve(int port, int cores) throws IOException {
    addr = new InetSocketAddress(port);
    bind(addr);
  }

  void serve(String host, int port) throws IOException {
    cores = Runtime.getRuntime().availableProcessors();
    addr = new InetSocketAddress(host, port);
    bind(addr);
  }

  void serve(String host, int port, int cores) throws IOException {
    addr = new InetSocketAddress(host, port);
    bind(addr);
  }

  void serve(InetAddress host, int port) throws IOException {
    cores = Runtime.getRuntime().availableProcessors();
    addr = new InetSocketAddress(host, port);
    bind(addr);
  }

  void serve(InetAddress host, int port, int cores) throws IOException {
    addr = new InetSocketAddress(host, port);
    bind(addr);
  }

}
