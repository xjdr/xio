package com.xjeffrose.xio.SSL;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.security.cert.*;
import javax.net.ssl.SSLException;

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
      return configure(config, builder).build();
    } catch (SSLException e) {
      return null;
    }
  }

  static public SslContext buildClientContext(TlsConfig config) {
    try {
      return configure(config, SslContextBuilder.forClient())
        .keyManager(config.getPrivateKey(), config.getCertificateAndTrustChain())
        .trustManager(config.getTrustChain())
        .build();
    } catch (SSLException e) {
      return null;
    }
  }
}
