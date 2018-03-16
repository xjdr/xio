package com.xjeffrose.xio.core;

import io.netty.channel.Channel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;
import lombok.val;

public class SocketAddressHelper {

  @Nullable
  public static String extractRemoteAddress(Channel channel) {
    if (channel instanceof ServerSocketChannel) {
      val inetSocketAddress = ((ServerSocketChannel) channel).remoteAddress();
      return computeAddress(inetSocketAddress);
    } else if (channel instanceof SocketChannel) {
      val inetSocketAddress = ((SocketChannel) channel).remoteAddress();
      return computeAddress(inetSocketAddress);
    } else {
      return null;
    }
  }

  private static String computeAddress(InetSocketAddress inetSocketAddress) {
    if (inetSocketAddress == null
        || inetSocketAddress.getAddress() == null
        || inetSocketAddress.getAddress().getHostAddress() == null) {
      return null;
    }

    val remoteInetAddress = inetSocketAddress.getAddress();
    val remoteHostAddress = remoteInetAddress.getHostAddress();
    val remoteAddressComponents = remoteHostAddress.trim().replace("/", "").split(":");

    if (remoteAddressComponents.length < 1) {
      return null;
    }

    return remoteAddressComponents[0];
  }
}
