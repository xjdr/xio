package com.xjeffrose.xio.server;

import java.net.InetSocketAddress;
import io.netty.channel.ChannelFuture;

abstract public class XioServerEndpoint {
  abstract public InetSocketAddress bindAddress();

  abstract public void afterBind(ChannelFuture channelFuture);
}
