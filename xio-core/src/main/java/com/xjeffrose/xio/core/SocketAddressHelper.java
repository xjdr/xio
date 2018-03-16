package com.xjeffrose.xio.core;

import io.netty.channel.Channel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;
import lombok.val;

public class SocketAddressHelper {

  @Nullable
  public String extractRemoteAddress(Channel channel) {
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

  @Nullable
  private String computeAddress(InetSocketAddress inetSocketAddress) {
    if (inetSocketAddress != null) {
      val inetAddress = inetSocketAddress.getAddress();
      if (inetAddress != null) {
        return inetAddress.getHostAddress();
      }
    }
    return null;
  }
}
