package com.xjeffrose.xio.server;

import com.xjeffrose.xio.core.XioNoOpHandler;
import com.xjeffrose.xio.server.XioSecurityFactory;
import com.xjeffrose.xio.server.XioSecurityHandlers;
import com.xjeffrose.xio.server.XioServerConfig;
import io.netty.channel.ChannelHandler;

public class XioNoOpSecurityFactory implements XioSecurityFactory {

  @Override
  public XioSecurityHandlers getSecurityHandlers() {
    return new XioSecurityHandlers() {
      @Override
      public ChannelHandler getAuthenticationHandler() {
        return new XioNoOpHandler();
      }

      @Override
      public ChannelHandler getEncryptionHandler() {
        return new XioNoOpHandler();
      }
    };
  }
}
