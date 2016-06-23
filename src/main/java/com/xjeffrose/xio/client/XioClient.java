package com.xjeffrose.xio.client;

import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.log4j.Logger;

public class XioClient implements Closeable {
  private static final Logger log = Logger.getLogger(XioClient.class);

  private final Bootstrap bootstrap;
  private final Node node;
  private final Distributor distributor;

  // This is just here to get the tests to compile
  public XioClient() {
    bootstrap = null;
    node = null;
    distributor = null;
  }

  public XioClient(String host, int port, ChannelHandler handler, boolean ssl) {
    this(new InetSocketAddress(host, port), handler, ssl);
  }

  public XioClient(String host, int port, Bootstrap bootstrap, boolean ssl) {
    this.bootstrap = bootstrap;
    this.node = new Node(new InetSocketAddress(host, port), bootstrap);
    this.distributor = null;
  }

  public XioClient(InetSocketAddress address, ChannelHandler handler, boolean ssl) {
    this.bootstrap = new XioClientBootstrap(handler, 4, ssl, Protocol.HTTPS).getBootstrap();
    this.node = new Node(address, bootstrap);
    this.distributor = null;
  }

  public XioClient(Distributor distributor, ChannelHandler handler, boolean ssl) {
    this.bootstrap = new XioClientBootstrap(handler, 4, ssl, Protocol.HTTPS).getBootstrap();
    this.node = null;
    this.distributor = distributor;
  }

  public boolean write(ByteBuf msg) {
    if (node == null) {
      return distributor.pick().send(msg);
    } else {
      return node.send(msg);
    }
  }

  @Override
  public void close() throws IOException {
    bootstrap.group().shutdownGracefully();
  }
}
