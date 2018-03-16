package com.xjeffrose.xio.core;

import io.netty.channel.ChannelHandlerContext;
import lombok.val;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SocketAddressHelper {
  @Nullable
  public static String extractRemoteAddress(SocketAddress socketAddress) {
    if(!(socketAddress instanceof InetSocketAddress))
    {
      return null;
    }

    val remoteAddress = ((InetSocketAddress) socketAddress).getAddress();
    if (remoteAddress == null || remoteAddress.getHostAddress() == null) {
      return null;
    }
    val remoteHostAddress = remoteAddress.getHostAddress();
    val remoteAddressComponents = remoteHostAddress.replace("/", "").split(":");

    if(remoteAddressComponents.length< 1)
    {
      return null;
    }

    return remoteAddressComponents[0];
  }
}
