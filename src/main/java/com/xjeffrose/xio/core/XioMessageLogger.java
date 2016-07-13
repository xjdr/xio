package com.xjeffrose.xio.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.logging.LoggingHandler;
import org.apache.log4j.Logger;

public class XioMessageLogger extends LoggingHandler {
  private static final Logger log = Logger.getLogger(XioMessageLogger.class);
  private static final char[] BYTE2CHAR;

  static {
    BYTE2CHAR = new char[256];
    for (int i = 0; i < BYTE2CHAR.length; ++i) {
      if (i > 31 && i < 127) {
        BYTE2CHAR[i] = (char) i;
      } else {
        BYTE2CHAR[i] = 46;
      }
    }
  }

  private static void appendPrettyHexDump(StringBuilder dump, ByteBuf buf) {
    int offset = buf.readerIndex();
    for (int i = 0; i < buf.readableBytes(); i++) {
      dump.append(BYTE2CHAR[buf.getUnsignedByte(offset + i)]);
    }
  }

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

  private void logMessageDebug(ChannelHandlerContext ctx, String eventName, Object msg) {
    log.debug(format(ctx, eventName, msg));
  }

  // for syslog, newline won't work, so the default pretty print logging format is messy
//  @Override
//  private static String formatByteBuf(ChannelHandlerContext ctx, String eventName, ByteBuf msg) {
//    int length = msg.readableBytes();
//    if (length == 0) {
//      StringBuilder rows1 = new StringBuilder(eventName.length() + 4);
//      rows1.append(eventName).append(": 0B");
//      return rows1.toString();
//    } else {
//      StringBuilder buf = new StringBuilder(eventName.length() + 12);
//      buf.append(eventName).append(": ").append(length).append('B');
//      appendPrettyHexDump(buf, msg);
//      return buf.toString();
//    }
//  }
}