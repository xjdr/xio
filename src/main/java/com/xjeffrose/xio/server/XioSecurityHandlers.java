package com.xjeffrose.xio.server;


import io.netty.channel.ChannelHandler;

public interface XioSecurityHandlers {
  ChannelHandler getAuthenticationHandler();

  ChannelHandler getEncryptionHandler();
}
