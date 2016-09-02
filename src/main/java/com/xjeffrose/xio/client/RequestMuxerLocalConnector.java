package com.xjeffrose.xio.client;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;

abstract public class RequestMuxerLocalConnector extends RequestMuxerConnector {

  @Override
  protected EventLoopGroup group() {
    return new LocalEventLoopGroup();
  }

  @Override
  protected Class<? extends Channel> channel () {
    return LocalChannel.class;
  }

  public RequestMuxerLocalConnector(LocalAddress address) {
    super(address);
  }

  public RequestMuxerLocalConnector(String address) {
    this(new LocalAddress(address));
  }

}
