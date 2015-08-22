package com.xjeffrose.xio.core;

import org.jboss.netty.channel.ChannelHandler;

public interface XioCodecFactory {

  public ChannelHandler getCodec();

}
