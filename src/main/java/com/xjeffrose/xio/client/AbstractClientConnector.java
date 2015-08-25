package com.xjeffrose.xio.client;

import com.google.common.net.HostAndPort;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;

public abstract class AbstractClientConnector<T extends XioClientChannel> implements XioClientConnector<T> {
  private final SocketAddress address;
  private final XioProtocolFactory protocolFactory;

  public AbstractClientConnector(SocketAddress address, XioProtocolFactory protocolFactory) {
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
  public ChannelFuture connect(ClientBootstrap bootstrap) {
    return bootstrap.connect(address);
  }

  @Override
  public String toString() {
    return address.toString();
  }

  protected XioProtocolFactory getProtocolFactory() {
    return protocolFactory;
  }
}