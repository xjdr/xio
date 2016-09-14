package com.xjeffrose.xio.mux;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;

import java.util.List;
import java.util.UUID;

public class Decoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    // uuid
    byte[] uuidBytes = new byte[36];
    in.readBytes(uuidBytes);
    UUID id = UUID.fromString(new String(uuidBytes));

    // op
    byte[] opBytes = new byte[4];
    in.readBytes(opBytes);
    Message.Op op = Message.Op.fromBytes(opBytes);

    // payloadSize
    byte[] payloadSizeBytes = new byte[4];
    in.readBytes(payloadSizeBytes);
    int payloadSize = Ints.fromByteArray(payloadSizeBytes);

    if (in.readableBytes() < payloadSize) {
      ctx.fireExceptionCaught(new DecoderException("Not enough bytes available to decode payload"));
    }

    out.add(in.readRetainedSlice(payloadSize));
    out.add(new Message(id, op));
  }
}
