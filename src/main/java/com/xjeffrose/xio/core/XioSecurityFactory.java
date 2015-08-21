package com.xjeffrose.xio.core;


import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;

public interface XioSecurityFactory {
  XioSecurityHandlers getSecurityHandlers(XioServerDef def, XioServerConfig serverConfig);
}