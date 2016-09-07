package com.xjeffrose.xio.core;

import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

public class FrameLengthCodec extends CombinedChannelDuplexHandler<LengthFieldBasedFrameDecoder, LengthFieldPrepender> {

  public FrameLengthCodec() {
    super(
      new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2),
      new LengthFieldPrepender(2)
    );
  }

}
