package com.xjeffrose.xio.server;


public interface XioSecurityFactory {
  XioSecurityHandlers getSecurityHandlers(XioServerDef def, XioServerConfig serverConfig);

  XioSecurityHandlers getSecurityHandlers();
}
