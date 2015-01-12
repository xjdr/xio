package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;

import com.xjeffrose.log.*;

class Server {
  private static final Logger log = Log.getLogger(Server.class.getName());

  private int port;
  private int cores;
  private InetAddress host;
  private InetSocketAddress addr;
  private ServerSocketChannel channel;
  private Acceptor acceptor;
  private IOService[] ioPool;

  Server() {
  }

  private void startIOService(int cores){
    ioPool = new IOService[cores];
    for (int i=0;i<cores;i++) {
      ioPool[i] = new IOService();
      ioPool[i].start();
    }
  }

  private void bind(InetSocketAddress addr) throws IOException {
    channel = ServerSocketChannel.open();
    channel.configureBlocking(false);
    channel.bind(addr);
    acceptor = getAcceptor();
    acceptor.ioPool(ioPool);
    acceptor.start();
    acceptor.register(channel);
  }

  private Acceptor getAcceptor() {
    Acceptor ac = new Acceptor();
    return ac;
  }

  /* private void announce(String path, Set<String> zkHosts) { */
  /* } */

  private void addRoute(String route, Service service) {
  }

  void serve(int port) throws IOException {
    cores = Runtime.getRuntime().availableProcessors();
    addr = new InetSocketAddress(port);
    startIOService(cores);
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
