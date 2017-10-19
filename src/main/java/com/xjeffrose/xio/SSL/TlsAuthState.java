package com.xjeffrose.xio.SSL;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public class TlsAuthState {

  private static final AttributeKey<String> PEER_IDENTITY_KEY = AttributeKey.newInstance("xio_peer_identity");

  public static final String UNAUTHENTICATED = "";

  public static void setPeerIdentity(ChannelHandlerContext ctx, String identity) {
    ctx.channel().attr(PEER_IDENTITY_KEY).set(identity);
  }

  private static String getDefaultPeer(String value) {
    if (value == null) {
      return UNAUTHENTICATED;
    }
    return value;
  }

  public static String getPeerIdentity(ChannelHandlerContext ctx) {
    return getDefaultPeer(ctx.channel().attr(PEER_IDENTITY_KEY).get());
  }

}
