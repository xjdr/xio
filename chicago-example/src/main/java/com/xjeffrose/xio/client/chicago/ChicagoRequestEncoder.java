package com.xjeffrose.xio.client.chicago;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * [LengthFieldBasedFrame [MumurHashedFrame [ChicagoMessage]]]
 *
 * <p>hash [4 bytes] msg [variable] id [36 bytes] op [4 bytes] colFamSize [4 bytes] colFam
 * [variable] keySize [4 bytes] key [variable] valSize [4 bytes] val [variable]
 */
public class ChicagoRequestEncoder extends MessageToByteEncoder<ChicagoMessage> {
  @Override
  protected void encode(ChannelHandlerContext ctx, ChicagoMessage msg, ByteBuf out) {
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
