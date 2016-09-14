package com.xjeffrose.xio.mux;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;

public class Encoder extends ChannelOutboundHandlerAdapter {

  private CompositeByteBuf currentMessage;
  private ByteBuf currentHeader;
  private boolean error = false;

  private void encodeMessage(Message msg, int payloadSize, ByteBuf out) {
    // uuid
    out.writeBytes(msg.id.toString().getBytes());
    // op
    out.writeBytes(Ints.toByteArray(msg.op.ordinal()));
    // payloadSize
    out.writeBytes(Ints.toByteArray(payloadSize));
  }

  private void reset() {
    currentMessage = null;
    currentHeader = null;
    error = false;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    if (msg instanceof ByteBuf) {
      if (currentMessage == null) {
        currentMessage = ctx.alloc().compositeBuffer();
        currentMessage.retain();
        currentHeader = ctx.alloc().buffer(40, 256);
        currentMessage.addComponent(currentHeader);
      }

      currentMessage.addComponent(true, (ByteBuf)msg);
    } else if (msg instanceof Message) {
      if (error) {
        reset();
        throw new EncoderException("Can only encode Message or ByteBuf");
      }
      if (currentMessage == null) {
        reset();
        throw new EncoderException("Encoder received Message without anything to encode");
      }

      encodeMessage((Message)msg, currentMessage.readableBytes(), currentHeader);
      currentMessage.addComponent(true, 0, currentHeader);

      ctx.write(currentMessage, promise);
      reset();
    } else {
      error = true;
    }
  }

}
