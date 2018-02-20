package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;

/** Used as a placeholder when the application codec is negotiated by ALPN. */
@ChannelHandler.Sharable
public class ApplicationCodecPlaceholderHandler extends ChannelHandlerAdapter {

  public static final ApplicationCodecPlaceholderHandler INSTANCE =
      new ApplicationCodecPlaceholderHandler();

  private ApplicationCodecPlaceholderHandler() {}
}
