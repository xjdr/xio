package com.xjeffrose.xio.SSL;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

public class SslContextFactory {

  static private SslContextBuilder configure(TlsConfig config, SslContextBuilder builder) {
    return builder
      .applicationProtocolConfig(config.getAlpnConfig())
      .ciphers(config.getCiphers(), SupportedCipherSuiteFilter.INSTANCE)
      .clientAuth(config.getClientAuth())
      .enableOcsp(config.isEnableOcsp())
      .protocols(config.getProtocols())
      .sessionCacheSize(config.getSessionCacheSize())
      .sessionTimeout(config.getSessionTimeout())
      .sslProvider(config.getSslProvider())
      ;
  }

  static public SslContext buildServerContext(TlsConfig config) {
    try {
      SslContextBuilder builder = SslContextBuilder
        .forServer(config.getPrivateKey(), config.getCertificateAndTrustChain());
      return configure(config, builder)
        // servers will trust only certs in trust chain
        .trustManager(config.getTrustChain())
        .build();
    } catch (SSLException e) {
      return null;
    }
  }

  static public SslContext buildClientContext(TlsConfig config, TrustManagerFactory trustManager) {
    try {
      return configure(config, SslContextBuilder.forClient())
        .keyManager(config.getPrivateKey(), config.getCertificateAndTrustChain())
        // clients will trust only certs in trust chain
        .trustManager(trustManager)
        .build();
    } catch (SSLException e) {
      return null;
    }
  }

  static TrustManagerFactory buildTrustManagerFactory(X509Certificate[] certCollection) {
    try {
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(null, null);

      int i = 1;
      for (X509Certificate cert: certCollection) {
        String alias = Integer.toString(i);
        ks.setCertificateEntry(alias, cert);
        i++;
      }

      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

      trustManagerFactory.init(ks);

      return trustManagerFactory;
    } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
      return null;
    }
  }

  static public SslContext buildClientContext(TlsConfig config) {
    return buildClientContext(config, buildTrustManagerFactory(config.getTrustChain()));
  }
}
