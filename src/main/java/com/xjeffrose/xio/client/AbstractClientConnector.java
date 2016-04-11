package com.xjeffrose.xio.client;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.strategies.RoundRobinLoadBalancer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Vector;


public abstract class AbstractClientConnector<T extends XioClientChannel> implements XioClientConnector<T> {
  private final SocketAddress address;
  private final XioProtocolFactory protocolFactory;
  private final Distributor pool;

  public AbstractClientConnector(Distributor pool, XioProtocolFactory protocolFactory) {
    this.pool = pool;
    this.address = null;
//    this.address = address;
    this.protocolFactory = protocolFactory;
  }

  public AbstractClientConnector(SocketAddress address, XioProtocolFactory protocolFactory) {
    final ImmutableList<Node> singletonPool = ImmutableList.of(new Node(address));

    this.pool = new Distributor(singletonPool, new RoundRobinLoadBalancer());
    this.address = address;
    this.protocolFactory = protocolFactory;
  }

  protected static SocketAddress toSocketAddress(HostAndPort address) {
    return new InetSocketAddress(address.getHostText(), address.getPort());
  }

  protected static XioProtocolFactory defaultProtocolFactory() {
    return new XioProtocolFactory();
  }

  @Override
  public ChannelFuture connect(Bootstrap bootstrap) {
    final Node node = pool.pick();
    final ChannelFuture cf = bootstrap.connect(node.address());

    node.addPending(cf.channel());
    return cf;
  }

  @Override
  public String toString() {
    return address.toString();
  }

  protected XioProtocolFactory getProtocolFactory() {
    return protocolFactory;
  }
}