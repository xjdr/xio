package com.xjeffrose.xio.client.chicago;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;

/** Verify a 32 bit murmur3 hash of a {@link ByteBuf} from the inbound pipeline. */
@Sharable
public class Murmur3HashedFrameDecoder extends MessageToMessageDecoder<ByteBuf> {
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    HashCode sentHash = HashCode.fromInt(msg.readInt());
    HashCode hash =
        Hashing.murmur3_32().hashBytes(msg.array(), msg.readerIndex(), msg.readableBytes());

    out.add(msg);

    if (sentHash.equals(hash)) {
      System.out.println("Hashes matches");
    } else {
      System.out.println("Hashes no matches");
    }
  }
}
