package com.xjeffrose.xio.core;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class XioNoOpSecurityFactory implements XioSecurityFactory {

  static final ChannelHandler noOpHandler = new SimpleChannelHandler() {
    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
      super.channelOpen(ctx, e);
      ctx.getPipeline().remove(this);
    }
  };

  @Override
  public XioSecurityHandlers getSecurityHandlers(HttpServerDef def, NettyServerConfig serverConfig) {
    return new XioSecurityHandlers() {
      @Override
      public ChannelHandler getAuthenticationHandler() {
        return noOpHandler;
      }

      @Override
      public ChannelHandler getEncryptionHandler() {
        return noOpHandler;
      }
    };
  }
}
