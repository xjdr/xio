package com.xjeffrose.log;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class Log {
  private static final Logger logger = Logger.getLogger(Log.class.getName());
  private static final Handler handler = new ConsoleHandler();
  private static Log instance;

  private Log() {
    handler.setFormatter(new LogFormatter());
    logger.setUseParentHandlers(false);
    logger.addHandler(handler);
  }

  public static Logger create() {
    if (instance == null) {
      instance = new Log();
    }
    return instance.logger;
  }
}

