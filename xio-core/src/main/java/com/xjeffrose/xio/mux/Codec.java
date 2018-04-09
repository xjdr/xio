package com.xjeffrose.xio.mux;

import io.netty.channel.CombinedChannelDuplexHandler;

public class Codec extends CombinedChannelDuplexHandler<Decoder, Encoder> {

  public Codec() {
    super(new Decoder(), new Encoder());
  }
}
