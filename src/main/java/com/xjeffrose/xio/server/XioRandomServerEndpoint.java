package com.xjeffrose.xio.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.InetSocketAddress;

public class XioRandomServerEndpoint extends XioServerEndpoint {
  private InetSocketAddress boundAddress;

  public InetSocketAddress bindAddress() {
    return new InetSocketAddress("127.0.0.1", 0);
  }

  public void afterBind(ChannelFuture channelFuture) {
    channelFuture.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
          boundAddress = (InetSocketAddress)future.channel().localAddress();
        }
      }
    });
  }

  public InetSocketAddress boundAddress() {
    return boundAddress;
  }

  public String hostAndPort() {
    return boundAddress.getHostString() + ":" + boundAddress.getPort();
  }
}
