package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Future;
import java.util.logging.*;

import com.xjeffrose.log.*;

class Server {
  private static final Logger log = Log.getLogger(Server.class.getName());

  private final Map<Route, Service> routes = new ConcurrentHashMap<Route, Service>();
  private final Promise<Server> serverPromise = new Promise<Server>();

  private final ChannelContextFactory factory = new ChannelContextFactory() {
    public ChannelContext build(SocketChannel channel) {
        return new ServerChannelContext(channel, routes);
    }
  };

  private int port;
  private int cores;
  private InetAddress host;
  private InetSocketAddress addr;
  private ServerSocketChannel channel;
  private Acceptor acceptor;
  private EventLoopPool pool;

  Server() {
  }

  private void schedule(ServerSocketChannel channel) {
    cores = Runtime.getRuntime().availableProcessors();
    pool = new EventLoopPool(cores);
    acceptor = new Acceptor(channel, pool, factory);
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
    routes.putIfAbsent(Route.build(route), service);
  }

  public void close() {
    try {
    acceptor.close();
    pool.close();
    channel.socket().close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Future<Server> serve(int port) throws IOException {
    return serve(port, 0);
  }

  public Future<Server> serve(int port, int cores) throws IOException {
    return serve(new InetSocketAddress(port), cores);
  }

  public Future<Server> serve(String host, int port) throws IOException {
    return serve(new InetSocketAddress(host, port), 0);
  }

  public Future<Server> serve(String host, int port, int cores) throws IOException {
    return serve(new InetSocketAddress(host, port), cores);
  }

  public Future<Server> serve(InetAddress host, int port) throws IOException {
    return serve(new InetSocketAddress(host, port), 0);
  }

  public Future<Server> serve(InetAddress host, int port, int cores) throws IOException {
    return serve(new InetSocketAddress(host, port), cores);
  }

  public Future<Server> serve(InetSocketAddress endpoint, int cores) throws IOException {
    if (cores == 0) {
      cores = Runtime.getRuntime().availableProcessors();
    }
    bind(endpoint);
    serverPromise.set(this);
    return serverPromise;
  }

}
