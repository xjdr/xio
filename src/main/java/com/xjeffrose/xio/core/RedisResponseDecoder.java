package com.xjeffrose.xio.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class RedisResponseDecoder extends ByteToMessageDecoder {

  private enum State {
    SimpleString ("+"),
    Error ("-"),
    Integer (":"),
    BulkString ("$"),
    Array ("*"),;

    private String val;

    private State(String val) {
      this.val = val;
    }
  }

  private static final String CRLF = "\r\n";

  @Override
  protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {

    ByteBuf _byteBuf = byteBuf.duplicate();

    int string = _byteBuf.forEachByte(new ByteBufProcessor() {
      @Override
      public boolean process(byte value) {
        return value != '+';
      }
    }


    int error = _byteBuf.forEachByte(new ByteBufProcessor() {
      @Override
      public boolean process(byte value) {
        return value != '-';
      }
    }


    int integer = _byteBuf.forEachByte(new ByteBufProcessor() {
      @Override
      public boolean process(byte value) {
        return value != ':';
      }
    }

    int bulkString = _byteBuf.forEachByte(new ByteBufProcessor() {
      @Override
      public boolean process(byte value) {
        return value != '$';
      }
    }

    int array = _byteBuf.forEachByte(new ByteBufProcessor() {
      @Override
      public boolean process(byte value) {
        return value != '*';
      }
    }

  }
}
