package com.xjeffrose.xio.mux;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class XioMuxEncoder extends MessageToByteEncoder {
  @Override
  protected void encode(ChannelHandlerContext ctx, Object o, ByteBuf byteBuf) throws Exception {

    byte[] msgSize = new byte[4];
    byte[] sessionId = new byte[4];
    byte[] type = new byte[4];
    byte[] source = new byte[8];
    byte[] destination = new byte[8];
    byte[] flags = new byte[8];
    ByteBuf msg = (ByteBuf) o;
    byte LF = '\0';

    byteBuf = ctx.alloc().directBuffer()
        .readBytes(msgSize)
        .readBytes(sessionId)
        .readBytes(type)
        .readBytes(source)
        .readBytes(destination)
        .readBytes(flags)
        .readBytes(msg)
        .readBytes(LF);
  }
}
