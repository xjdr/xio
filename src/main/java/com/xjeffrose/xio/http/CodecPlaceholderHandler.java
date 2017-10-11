package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;

/**
 * Used as a placeholder when the codec is negotiated by ALPN.
 */

@ChannelHandler.Sharable
public class CodecPlaceholderHandler extends ChannelHandlerAdapter {

  public static final CodecPlaceholderHandler INSTANCE = new CodecPlaceholderHandler();

  private CodecPlaceholderHandler() {
  }

}
