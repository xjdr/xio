package com.xjeffrose.xio.mux;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;

abstract public class LocalConnector extends Connector {

  @Override
  protected EventLoopGroup group() {
    return new LocalEventLoopGroup();
  }

  @Override
  protected Class<? extends Channel> channel () {
    return LocalChannel.class;
  }

  public LocalConnector(LocalAddress address) {
    super(address);
  }

  public LocalConnector(String address) {
    this(new LocalAddress(address));
  }

}
