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

  private static SslContextBuilder configure(TlsConfig config, SslContextBuilder builder) {
    return builder
        .applicationProtocolConfig(config.getAlpnConfig())
        .ciphers(config.getCiphers(), SupportedCipherSuiteFilter.INSTANCE)
        .clientAuth(config.getClientAuth())
        .enableOcsp(config.isEnableOcsp())
        .protocols(config.getProtocols())
        .sessionCacheSize(config.getSessionCacheSize())
        .sessionTimeout(config.getSessionTimeout())
        .sslProvider(config.getSslProvider());
  }

  private static SslContextBuilder newServerBuilder(TlsConfig config) {
    return SslContextBuilder.forServer(config.getPrivateKey(), config.getCertificateAndChain());
  }

  public static SslContext buildServerContext(
      TlsConfig config, TrustManagerFactory trustManager, boolean allowExpiredClients) {
    try {
      return configure(config, newServerBuilder(config))
          .trustManager(new XioTrustManagerFactory(trustManager, allowExpiredClients))
          .build();
    } catch (SSLException e) {
      return null;
    }
  }

  public static SslContext buildServerContext(TlsConfig config, TrustManagerFactory trustManager) {
    return buildServerContext(config, trustManager, false);
  }

  public static SslContext buildServerContext(TlsConfig config, boolean allowExpiredClients) {
    // servers will trust only certs in trusted certs collection
    return buildServerContext(
        config, buildTrustManagerFactory(config.getTrustedCerts()), allowExpiredClients);
  }

  public static SslContext buildServerContext(TlsConfig config) {
    return buildServerContext(config, false);
  }

  public static SslContext buildClientContext(TlsConfig config, TrustManagerFactory trustManager) {
    try {
      return configure(config, SslContextBuilder.forClient())
          .keyManager(config.getPrivateKey(), config.getCertificateAndChain())
          .trustManager(new XioTrustManagerFactory(trustManager))
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
      for (X509Certificate cert : certCollection) {
        String alias = Integer.toString(i);
        ks.setCertificateEntry(alias, cert);
        i++;
      }

      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

      trustManagerFactory.init(ks);

      return trustManagerFactory;
    } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
      return null;
    }
  }

  public static SslContext buildClientContext(TlsConfig config) {
    // clients will trust only certs in trusted certs collection
    return buildClientContext(config, buildTrustManagerFactory(config.getTrustedCerts()));
  }
}
