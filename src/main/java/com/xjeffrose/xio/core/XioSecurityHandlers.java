package com.xjeffrose.xio.core;


import io.netty.channel.ChannelHandler;

public interface XioSecurityHandlers {
  ChannelHandler getAuthenticationHandler();

  ChannelHandler getEncryptionHandler();

  ChannelHandler getEncryptionHandler(boolean clientMode);
}
