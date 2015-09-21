package com.xjeffrose.xio.core;

import io.airlift.log.Logger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LoggingHandler;

public class XioExceptionLogger extends LoggingHandler {
  private static final Logger log = Logger.get(XioExceptionLogger.class);

  private void logMessage(ChannelHandlerContext ctx, String eventName, Object msg) {
    log.error(format(ctx, formatMessage(eventName, msg)));
  }
}