package com.xjeffrose.xio.client;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RequestMuxerEncoder extends MessageToByteEncoder<RequestMuxerMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, RequestMuxerMessage msg, ByteBuf out) {
    // uuid
    out.writeBytes(msg.id.toString().getBytes());
    // op
    out.writeBytes(Ints.toByteArray(msg.op.ordinal()));
    // colFamSize
    out.writeBytes(Ints.toByteArray(msg.colFam.getBytes().length));
    // colFam
    out.writeBytes(msg.colFam.getBytes());
    // keySize
    out.writeBytes(Ints.toByteArray(msg.key.getBytes().length));
    // key
    out.writeBytes(msg.key.getBytes());
    // valSize
    out.writeBytes(Ints.toByteArray(msg.val.getBytes().length));
    // val
    out.writeBytes(msg.val.getBytes());
  }

}
