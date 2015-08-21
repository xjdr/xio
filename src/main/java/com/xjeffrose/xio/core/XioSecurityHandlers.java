package com.xjeffrose.xio.core;


import org.jboss.netty.channel.ChannelHandler;

public interface XioSecurityHandlers {
  ChannelHandler getAuthenticationHandler();

  ChannelHandler getEncryptionHandler();
}
