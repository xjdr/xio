package com.xjeffrose.xio.tls;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

public class MutualAuthHandler extends ChannelInboundHandlerAdapter {

  private String getPeerIdentity(SSLEngine engine) {
    try {
      SSLSession session = engine.getSession();
      javax.security.cert.X509Certificate[] chain = session.getPeerCertificateChain();
      if (chain == null || chain.length == 0 || chain[0] == null) {
        return TlsAuthState.UNAUTHENTICATED;
      }
      // double check that the certificate is valid
      chain[0].checkValidity();

      return session.getPeerPrincipal().getName();
    } catch (javax.security.cert.CertificateExpiredException
        | javax.security.cert.CertificateNotYetValidException e) {
      return TlsAuthState.UNAUTHENTICATED;
    } catch (SSLPeerUnverifiedException e) {
      return TlsAuthState.UNAUTHENTICATED;
    }
  }

  public void peerIdentityEstablished(ChannelHandlerContext ctx, String identity) {}

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof SslHandshakeCompletionEvent) {
      ctx.pipeline().remove(this);

      SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
      String peerIdentity = TlsAuthState.UNAUTHENTICATED;
      if (handshakeEvent.isSuccess()) {
        SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
        if (sslHandler == null) {
          throw new IllegalStateException(
              "cannot find a SslHandler in the pipeline (required for MutualAuthHandler)");
        }
        peerIdentity = getPeerIdentity(sslHandler.engine());
      }
      TlsAuthState.setPeerIdentity(ctx, peerIdentity);
      peerIdentityEstablished(ctx, peerIdentity);
    }

    ctx.fireUserEventTriggered(evt);
  }
}
