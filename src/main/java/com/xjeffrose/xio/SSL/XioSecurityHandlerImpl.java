package com.xjeffrose.xio.SSL;

import com.xjeffrose.xio.core.XioNoOpSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;


public class XioSecurityHandlerImpl implements XioSecurityHandlers {
  private static final String PASSWORD = "passwordsAreGood";

  private final String cert;
  private final String key;
  private final boolean clientMode;
  private final static X509Certificate selfSignedCert = createSelfSigned();


  public static X509Certificate createSelfSigned(){
    try{
      return SelfSignedX509CertGenerator.generate("*.paypal.com");
    }catch (Exception e){
      e.printStackTrace();
    }
    return null;
  }


  public XioSecurityHandlerImpl() {
    this(false);
  }


  public XioSecurityHandlerImpl(boolean clientMode) {
    this(null, null, clientMode);
  }

  public XioSecurityHandlerImpl(String cert, String key) {
    this(cert, key, false);
  }


  public XioSecurityHandlerImpl(String cert, String key, boolean clientMode) {
    this.cert = cert;
    this.key = key;
    this.clientMode = clientMode;
  }

  @Override
  public ChannelHandler getAuthenticationHandler() {
    return new XioNoOpSecurityFactory().getSecurityHandlers(null, null).getAuthenticationHandler();
  }

  @Override
  public ChannelHandler getEncryptionHandler() {
    try {

      final List<java.security.cert.X509Certificate> certList = new ArrayList<>();
      final String rawCertString = cert;
      PrivateKey privateKey;
      PublicKey publicKey;

      if (key != null) {
        X509CertificateGenerator.DERKeySpec derKeySpec = X509CertificateGenerator.parseDERKeySpec(key);
        privateKey = X509CertificateGenerator.buildPrivateKey(derKeySpec);
        publicKey = X509CertificateGenerator.buildPublicKey(derKeySpec);
      } else {
        privateKey = selfSignedCert.getKey();
      }

      java.security.cert.X509Certificate[] chain;

      if (cert != null) {

        String[] certs = rawCertString.split("-----END CERTIFICATE-----\n");

        for (String cert : certs) {
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          java.security.cert.X509Certificate x509Certificate =
              (java.security.cert.X509Certificate) cf.generateCertificate(
                  new ByteArrayInputStream((cert + "-----END CERTIFICATE-----\n").getBytes()));
          certList.add(x509Certificate);
        }

        chain = new java.security.cert.X509Certificate[certList.size()];

        for (int i = 0; i < certList.size(); i++) {
          chain[i] = certList.get(i);
        }
      } else {
        chain = new java.security.cert.X509Certificate[1];
        chain[0] = selfSignedCert.getCert();
      }
      if (!clientMode) {

        SslContext sslCtx;

        if (OpenSsl.isAvailable()) {
          sslCtx = SslContextBuilder
              .forServer(privateKey, chain)
              .sslProvider(SslProvider.OPENSSL)
              .build();
        } else {
          final KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");
          keyStore.load(null, PASSWORD.toCharArray());
          keyStore.setKeyEntry(chain[0].getIssuerX500Principal().getName(), privateKey, PASSWORD.toCharArray(), chain);
          KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

          kmf.init(keyStore, PASSWORD.toCharArray());
          sslCtx = SslContextBuilder
              .forServer(kmf)
              .sslProvider(SslProvider.JDK)
              .build();
        }

        ChannelHandler handler = sslCtx.newHandler(new PooledByteBufAllocator());

        //
        // tsu - enable all protocols since legacy apps still use SSLv2Hello...
        // JDK 7, 8, 9 (Early Access)    SSLv2Hello(2), SSLv3, TLSv1, TLSv1.1, TLSv1.2
        // TODO(JR): Fix this or only enable for certain service as this is insecure
        ((SslHandler) handler).engine().setEnabledProtocols(((SslHandler) handler).engine().getSupportedProtocols());

        return handler;

      } else {
        SslContext sslCtx;

        if (OpenSsl.isAvailable()) {
          sslCtx = SslContextBuilder
              .forClient()
              .sslProvider(SslProvider.OPENSSL)
              .keyManager(privateKey, chain)
              .trustManager(InsecureTrustManagerFactory.INSTANCE)
              .build();
        } else {
          final KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");
          keyStore.load(null, PASSWORD.toCharArray());
          keyStore.setKeyEntry(chain[0].getIssuerX500Principal().getName(), privateKey, PASSWORD.toCharArray(), chain);
          KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");

          kmf.init(keyStore, PASSWORD.toCharArray());
          sslCtx = SslContextBuilder
              .forClient()
              .sslProvider(SslProvider.JDK)
              .keyManager(kmf)
              .trustManager(InsecureTrustManagerFactory.INSTANCE)
              .build();
        }

        ChannelHandler handler = sslCtx.newHandler(new PooledByteBufAllocator());

        return handler;
      }

    } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | CertificateException | NoSuchProviderException | IllegalArgumentException | IOException  e) {
      e.printStackTrace();
    }

    return null;
  }
}