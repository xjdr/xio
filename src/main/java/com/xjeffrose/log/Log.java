package com.xjeffrose.log;

import java.util.logging.*;

public class Log {
  private static Logger logger;
  private static Handler handler;

  private Log() {}

  public static Logger getLogger(String className) {
    logger = Logger.getLogger(className);
    handler = new ConsoleHandler();
    handler.setFormatter(new LogFormatter());
    handler.setLevel(Level.ALL);
    handler.setFilter(null);
    logger.setUseParentHandlers(false);
    logger.addHandler(handler);

    return logger;
  }
}




