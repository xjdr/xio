package com.xjeffrose.xio.fixtures;

import com.xjeffrose.xio.core.XioNoOpHandler;
import com.xjeffrose.xio.server.XioSecurityFactory;
import com.xjeffrose.xio.server.XioSecurityHandlers;
import com.xjeffrose.xio.server.XioServerConfig;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLException;

public class XioTestSecurityFactory implements XioSecurityFactory {
  @Override
  public XioSecurityHandlers getSecurityHandlers() {
    return new XioSecurityHandlers() {
      @Override
      public ChannelHandler getAuthenticationHandler() {
        return new XioNoOpHandler();
      }

      @Override
      public ChannelHandler getEncryptionHandler() {
        try {
          SelfSignedCertificate ssc = new SelfSignedCertificate();
          SslContext sslCtx = SslContextBuilder.forClient()
              .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

//                SSLEngine engine = new SSLEngineFactory("src/test/resources/privateKey.pem", "src/test/resources/cert.pem").getEngine();
//                engine.beginHandshake();

          return sslCtx.newHandler(new PooledByteBufAllocator());

        } catch (SSLException | CertificateException e) {
          e.printStackTrace();
        }
//                return new SslHandler(engine);
        return null;
      }
    };
  }
}
