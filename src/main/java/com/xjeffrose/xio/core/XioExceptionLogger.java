package com.xjeffrose.xio.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LoggingHandler;
import org.apache.log4j.Logger;

public class XioExceptionLogger extends LoggingHandler {
  private static final Logger log = Logger.getLogger(XioExceptionLogger.class);

  private void logMessage(ChannelHandlerContext ctx, String eventName, Object msg) {
    log.error(format(ctx, formatMessage(eventName, msg)));
  }
}