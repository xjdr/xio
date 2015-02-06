package com.xjeffrose.xio;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Future;
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
  private EventLoopPool pool;

  public ByteBuffer[] req;

  Client() {
    cores = Runtime.getRuntime().availableProcessors();
    pool = new EventLoopPool(cores);
  }

  private void schedule(Connector connector) {
    connector.start();
    pool.start();
  }

  private Connector connect(InetSocketAddress addr) throws IOException {
    channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.connect(addr);
    Connector connector = new Connector(channel, pool, req);
    schedule(connector);
    return connector;
  }

  /* private void announce(String path, Set<String> zkHosts) { */
  /* } */

  public void defaultRequest() {
    req = HttpRequest.RequestBuilder.newBuilder()
                         .method("GET")
                         .path("/")
                         .protocol("HTTP/1.1")
                         .addHeader("User-Agent", "xio/0.1")
                         .addHeader("Host", "localhost:8000")
                         .addHeader("Accept", "*/*")
                         .build();
  }

  public void request() {
    req = HttpRequest.RequestBuilder.newBuilder()
                         .method("GET")
                         .path("/")
                         .protocol("HTTP/1.1")
                         .addHeader("User-Agent", "xio/0.1")
                         .addHeader("Host", "localhost:8000")
                         .addHeader("Accept", "*/*")
                         .build();
  }

  Future<HttpResponse> get(int port) throws IOException {
    addr = new InetSocketAddress(port);
    Connector connector = connect(addr);
    return connector.getResponse();
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
    addr = new InetSocketAddress(host, port);
    connect(addr);
  }

  void get(InetAddress host, int port, int cores) throws IOException {
    addr = new InetSocketAddress(host, port);
    connect(addr);
  }

}
