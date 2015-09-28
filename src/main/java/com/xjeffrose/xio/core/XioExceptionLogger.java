package com.xjeffrose.xio.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import org.apache.log4j.Logger;

public class XioExceptionLogger extends LoggingHandler {
  private static final Logger log = Logger.getLogger(XioExceptionLogger.class);

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    this.logMessageDebug(ctx, "RECEIVED", msg);
    ctx.fireChannelRead(msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    this.logMessageDebug(ctx, "WRITE", msg);
    ctx.write(msg, promise);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    this.logMessage(ctx, "EXCEPTION: " + cause, null);
  }

  private void logMessageDebug(ChannelHandlerContext ctx, String eventName, Object msg) {
    log.debug(format(ctx, formatMessage(eventName, msg)));
  }

  private void logMessage(ChannelHandlerContext ctx, String eventName, Object msg) {
    log.error(format(ctx, formatMessage(eventName, msg)));
  }

}