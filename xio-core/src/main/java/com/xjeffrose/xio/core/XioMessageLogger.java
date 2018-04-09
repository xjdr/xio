package com.xjeffrose.xio.core;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

// TODO(CK): Consider renaming this to either MessageLogger or XioLoggingHandler
/** Utility class for building a LoggingHandler with an instance specific name */
public class XioMessageLogger extends LoggingHandler {

  public XioMessageLogger(Class<?> clazz, String name) {
    super(String.format("%s.%s", clazz.getName(), name));
  }

  public XioMessageLogger(Class<?> clazz, String name, LogLevel level) {
    super(String.format("%s.%s", clazz.getName(), name), level);
  }
}
