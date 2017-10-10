package com.xjeffrose.xio.SSL;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;

public class SslContextFactory {

  static private SslContextBuilder configure(TlsConfig config, SslContextBuilder builder) {
    return builder
      .applicationProtocolConfig(config.getAlpnConfig())
      .ciphers(config.getCiphers(), SupportedCipherSuiteFilter.INSTANCE)
      .clientAuth(config.getClientAuth())
      .enableOcsp(config.isEnableOcsp())
      //.keyManager
      .protocols(config.getProtocols())
      .sessionCacheSize(config.getSessionCacheSize())
      .sessionTimeout(config.getSessionTimeout())
      //.trustManager
      .sslProvider(config.getSslProvider())
      ;
  }

  static public SslContext buildServerContext(TlsConfig config) {
    try {
      SslContextBuilder builder = SslContextBuilder
        .forServer(config.getPrivateKey(), config.getCertificateAndTrustChain());
      return configure(config, builder).build();
    } catch (SSLException e) {
      return null;
    }
  }

  static public SslContext buildClientContext(TlsConfig config) {
    try {
      return configure(config, SslContextBuilder.forClient()).build();
    } catch (SSLException e) {
      return null;
    }
  }
}
