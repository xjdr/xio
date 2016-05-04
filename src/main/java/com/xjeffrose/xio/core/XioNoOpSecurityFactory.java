package com.xjeffrose.xio.core;

import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import io.netty.channel.ChannelHandler;

public class XioNoOpSecurityFactory implements XioSecurityFactory {

  @Override
  public XioSecurityHandlers getSecurityHandlers(XioServerDef def, XioServerConfig serverConfig) {
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
