package com.xjeffrose.xio.server;

import com.google.inject.Provider;

public class XioServerConfigProvider implements Provider<XioServerConfig> {
  @Override
  public XioServerConfig get() {
    return XioServerConfig.newBuilder().build();
  }
}
