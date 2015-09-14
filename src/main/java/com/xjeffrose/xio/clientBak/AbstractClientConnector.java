package com.xjeffrose.xio.clientBak;

import com.google.common.net.HostAndPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import java.net.SocketAddress;


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
  public ChannelFuture connect(Bootstrap bootstrap) {
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