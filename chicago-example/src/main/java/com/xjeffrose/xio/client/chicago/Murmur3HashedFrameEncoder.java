package com.xjeffrose.xio.client.chicago;

import com.google.common.hash.Hashing;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;

/** Prepend a 32 bit murmur3 hash of a {@link ByteBuf} to the outbound pipeline. */
@Sharable
public class Murmur3HashedFrameEncoder extends MessageToMessageEncoder<ByteBuf> {
  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    // allocate a 4 byte (32 bit) buffer
    // using the same allocator as the provided ByteBuf
    ByteBuf hash = msg.alloc().buffer(4);
    hash.writeBytes(
        Hashing.murmur3_32()
            .hashBytes(msg.array(), msg.readerIndex(), msg.readableBytes())
            .asBytes());
    out.add(hash);
    out.add(msg);
  }
}
