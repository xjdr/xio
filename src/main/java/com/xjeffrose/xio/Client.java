package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;

import com.xjeffrose.log.*;

//TODO: Add support for tuples of hosts for round robin LB
//TODO: Add support for pull based / latency based LB algo
//TODO: Add support for GET and POST methods

class Client {
  private static final Logger log = Log.getLogger(Client.class.getName());

  private int port;
  private int cores;
  private InetAddress host;
  private InetSocketAddress addr;
  private SocketChannel channel;

  Client() {
  }

  private void schedule(SocketChannel channel) {
    cores = Runtime.getRuntime().availableProcessors();
    EventLoopPool pool = new EventLoopPool(cores);
    Connector connector = new Connector(channel, pool);
    connector.start();
    pool.start();
  }

  private void connect(InetSocketAddress addr) throws IOException {
    channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.connect(addr);
    schedule(channel);
  }

  /* private void announce(String path, Set<String> zkHosts) { */
  /* } */

  void get(int port) throws IOException {
    cores = Runtime.getRuntime().availableProcessors();
    addr = new InetSocketAddress(port);
    connect(addr);
  }

  void get(int port, int cores) throws IOException {
    addr = new InetSocketAddress(port);
    connect(addr);
  }

  void get(String host, int port) throws IOException {
    cores = Runtime.getRuntime().availableProcessors();
    addr = new InetSocketAddress(host, port);
    connect(addr);
  }

  void get(String host, int port, int cores) throws IOException {
    addr = new InetSocketAddress(host, port);
    connect(addr);
  }

  void get(InetAddress host, int port) throws IOException {
    cores = Runtime.getRuntime().availableProcessors();
    addr = new InetSocketAddress(host, port);
    connect(addr);
  }

  void get(InetAddress host, int port, int cores) throws IOException {
    addr = new InetSocketAddress(host, port);
    connect(addr);
  }

}
