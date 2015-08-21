package com.xjeffrose.xio.core;

import com.google.inject.Provider;

public class DefaultNettyServerConfigProvider implements Provider<NettyServerConfig> {
  @Override
  public NettyServerConfig get() {
    return NettyServerConfig.newBuilder().build();
  }
}
