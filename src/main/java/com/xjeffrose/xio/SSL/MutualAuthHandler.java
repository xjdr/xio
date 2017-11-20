package com.xjeffrose.xio.SSL;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import java.security.cert.*;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

public class MutualAuthHandler extends ChannelInboundHandlerAdapter {

  private String getPeerIdentity(SSLEngine engine) {
    try {
      SSLSession session = engine.getSession();
      return session.getPeerPrincipal().getName();
    } catch (SSLPeerUnverifiedException e) {
      return TlsAuthState.UNAUTHENTICATED;
    }
  }

  public void peerIdentityEstablished(ChannelHandlerContext ctx, String identity) {
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof SslHandshakeCompletionEvent) {
      ctx.pipeline().remove(this);

      SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
      String peerIdentity = TlsAuthState.UNAUTHENTICATED;
      if (handshakeEvent.isSuccess()) {
        SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
        if (sslHandler == null) {
          throw new IllegalStateException("cannot find a SslHandler in the pipeline (required for MutualAuthHandler)");
        }
        peerIdentity = getPeerIdentity(sslHandler.engine());
      }
      TlsAuthState.setPeerIdentity(ctx, peerIdentity);
      peerIdentityEstablished(ctx, peerIdentity);
    }

    ctx.fireUserEventTriggered(evt);
  }
}
