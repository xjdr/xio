package com.xjeffrose.xio.pipeline;

import io.netty.channel.ChannelHandler;

public interface XioChannelHandlerFactory {

  public ChannelHandler build();

}
