package com.xjeffrose.xio.core;

import io.netty.channel.ChannelHandler;

public interface XioCodecFactory {

  ChannelHandler getCodec();
}
