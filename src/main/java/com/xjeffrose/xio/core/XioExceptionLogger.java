package com.xjeffrose.xio.core;

import io.airlift.log.Logger;
import java.net.SocketAddress;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.logging.LoggingHandler;

public class XioExceptionLogger extends LoggingHandler {
  private static final Logger log = Logger.get(XioExceptionLogger.class);

  @Override
  public void log(ChannelEvent event) {
    if (event instanceof ExceptionEvent) {
      ExceptionEvent exceptionEvent = (ExceptionEvent) event;
      SocketAddress remoteAddress = exceptionEvent.getChannel().getRemoteAddress();
      log.error(exceptionEvent.getCause(), "Exception triggered on channel connected to %s", remoteAddress);
    }
  }
}