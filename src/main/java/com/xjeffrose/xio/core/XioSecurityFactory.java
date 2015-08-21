package com.xjeffrose.xio.core;


public interface XioSecurityFactory {
  XioSecurityHandlers getSecurityHandlers(HttpServerDef def, NettyServerConfig serverConfig);
}