package com.xjeffrose.xio.client.mux;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class Decoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    // uuid
    byte[] uuidBytes = new byte[36];
    in.readBytes(uuidBytes);
    // op
    byte[] opBytes = new byte[4];
    in.readBytes(opBytes);
    // colFamSize
    byte[] colFamSizeBytes = new byte[4];
    in.readBytes(colFamSizeBytes);
    // colFam
    byte[] colFamBytes = new byte[Ints.fromByteArray(colFamSizeBytes)];
    in.readBytes(colFamBytes);
    // keySize
    byte[] keySizeBytes = new byte[4];
    in.readBytes(keySizeBytes);
    // key
    byte[] keyBytes = new byte[Ints.fromByteArray(keySizeBytes)];
    in.readBytes(keyBytes);
    // valSize
    byte[] valSizeBytes = new byte[4];
    in.readBytes(valSizeBytes);
    // val
    byte[] valBytes = new byte[Ints.fromByteArray(valSizeBytes)];
    in.readBytes(valBytes);

    out.add(new Message(uuidBytes, opBytes, colFamBytes, keyBytes, valBytes));
  }
}
