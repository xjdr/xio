package com.xjeffrose.xio.tls;

import com.typesafe.config.ConfigFactory;
import io.netty.handler.ssl.SslContext;
import org.junit.Assert;
import org.junit.Test;

public class SslContextFactoryUnitTest extends Assert {

  @Test
  public void buildServerContext() throws Exception {
    TlsConfig tlsConfig = TlsConfig.builderFrom(ConfigFactory.load("tls-reference.conf")).build();
    SslContext context = SslContextFactory.buildServerContext(tlsConfig);
    assertNotNull(context);
  }

  @Test
  public void buildClientContext() throws Exception {
    TlsConfig tlsConfig = TlsConfig.builderFrom(ConfigFactory.load("tls-reference.conf")).build();
    SslContext context = SslContextFactory.buildClientContext(tlsConfig);
    assertNotNull(context);
  }
}
