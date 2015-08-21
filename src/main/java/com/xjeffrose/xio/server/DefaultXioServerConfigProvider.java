package com.xjeffrose.xio.server;

import com.google.inject.Provider;
import com.xjeffrose.xio.server.XioServerConfig;

public class DefaultXioServerConfigProvider implements Provider<XioServerConfig> {
  @Override
  public XioServerConfig get() {
    return XioServerConfig.newBuilder().build();
  }
}
