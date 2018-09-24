package com.xjeffrose.xio.SSL;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import com.typesafe.config.ConfigFactory;
import io.netty.handler.ssl.SslContext;
import org.junit.Assert;
import org.junit.Test;

public class SslContextFactoryUnitTest extends Assert {

  @Test
  public void buildServerContext() throws Exception {
    TlsConfig tlsConfig =
        TlsConfig.builderFrom(ConfigFactory.load().getConfig("xio.testServer.settings.tls"))
            .build();
    SslContext context = SslContextFactory.buildServerContext(tlsConfig);
  }

  @Test
  public void buildClientContext() throws Exception {
    TlsConfig tlsConfig =
        TlsConfig.builderFrom(ConfigFactory.load().getConfig("xio.testServer.settings.tls"))
            .build();
    SslContext context = SslContextFactory.buildClientContext(tlsConfig);
  }
}
