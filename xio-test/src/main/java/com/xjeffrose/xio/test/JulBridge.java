package com.xjeffrose.xio.test;

import org.slf4j.bridge.SLF4JBridgeHandler;

public abstract class JulBridge {
  private static int dummy = -1;

  public static synchronized void initialize() {
    if (dummy == -1) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
      dummy = 0;
    }
  }
}
