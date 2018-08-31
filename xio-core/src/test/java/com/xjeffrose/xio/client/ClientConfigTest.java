package com.xjeffrose.xio.client;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.netty.channel.ChannelOption;
import java.net.InetSocketAddress;
import java.util.Map;
import org.assertj.core.util.Maps;
import org.junit.Assert;
import org.junit.Test;

public class ClientConfigTest extends Assert {
  @Test
  public void testBuilderCustomValues() {
    Config config = ConfigFactory.load();
    ClientConfig fallbackObject = ClientConfig.from(config.getConfig("xio.clientTemplate"));
    Map expectedBootstrapOptions = Maps.newHashMap(ChannelOption.AUTO_READ, new Object());
    String expectedName = "name";
    TlsConfig expectedTls = new TlsConfig(ConfigFactory.load("tls-reference"));
    boolean expectedMessageLoggerEnabled = true;
    InetSocketAddress expectedLocal = new InetSocketAddress("local ip", 11111);
    InetSocketAddress expectedRemote = new InetSocketAddress("remote ip", 22222);
    IdleTimeoutConfig expectedIdleTimeoutConfig = new IdleTimeoutConfig(true, 55);

    ClientConfig subject =
        ClientConfig.newBuilder(fallbackObject)
            .setBootstrapOptions(expectedBootstrapOptions)
            .setName(expectedName)
            .setTls(expectedTls)
            .setMessageLoggerEnabled(expectedMessageLoggerEnabled)
            .setLocal(expectedLocal)
            .setRemote(expectedRemote)
            .setIdleTimeoutConfig(expectedIdleTimeoutConfig)
            .build();

    assertEquals(expectedBootstrapOptions, subject.bootstrapOptions());
    assertEquals(expectedTls, subject.tls());
    assertEquals(expectedMessageLoggerEnabled, subject.messageLoggerEnabled());
    assertEquals(expectedLocal, subject.local());
    assertEquals(expectedRemote, subject.remote());
    assertEquals(expectedIdleTimeoutConfig, subject.idleTimeoutConfig());
  }

  @Test
  public void testBuilderFallbackValues() {
    ClientConfig fallbackObject =
        ClientConfig.from(ConfigFactory.load().getConfig("xio.clientTemplate"));
    ClientConfig subject = ClientConfig.newBuilder(fallbackObject).build();

    assertNotNull(subject);
  }
}
