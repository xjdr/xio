package com.xjeffrose.xio.http;

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.client.ClientConfig;
import io.netty.handler.codec.http.HttpMethod;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;

public class ProxyRouteConfigTest extends Assert {
  @Test
  public void testBuilderCustomValues() {
    ProxyRouteConfig fallbackObject =
        new ProxyRouteConfig(ConfigFactory.load().getConfig("xio.proxyRouteTemplate"));
    List<HttpMethod> expectedMethods = Lists.newArrayList(HttpMethod.CONNECT);
    String expectedHost = "host";
    String expectedPath = "/path/";
    String expectedPermissionNeeded = "permissionNeeded";
    List<ClientConfig> expectedClientConfigs =
        Lists.newArrayList(
            ClientConfig.newBuilder(
                    ClientConfig.from(ConfigFactory.load().getConfig("xio.clientTemplate")))
                .setName("client config name")
                .build());
    ProxyRouteConfig.ProxyHostPolicy expectedProxyHostPolicy =
        ProxyRouteConfig.ProxyHostPolicy.UseConfigValue;
    String expectedProxyHost = "proxyHost";
    String expectedProxyPath = "/proxyPath/";

    ProxyRouteConfig subject =
        ProxyRouteConfig.newBuilder(fallbackObject)
            .setMethods(expectedMethods)
            .setHost(expectedHost)
            .setPath(expectedPath)
            .setPermissionNeeded(expectedPermissionNeeded)
            .setClientConfigs(expectedClientConfigs)
            .setProxyHostPolicy(expectedProxyHostPolicy)
            .setProxyHost(expectedProxyHost)
            .setProxyPath(expectedProxyPath)
            .build();

    assertEquals(expectedMethods, subject.methods());
    assertEquals(expectedHost, subject.host());
    assertEquals(expectedPath, subject.path());
    assertEquals(expectedPermissionNeeded, subject.permissionNeeded());
    assertEquals(expectedClientConfigs, subject.clientConfigs());
    assertEquals(expectedProxyHostPolicy, subject.proxyHostPolicy());
    assertEquals(expectedProxyHost, subject.proxyHost());
    assertEquals(expectedProxyPath, subject.proxyPath());
  }

  @Test
  public void testBuilderFallbackValues() {
    ProxyRouteConfig fallbackObject =
        new ProxyRouteConfig(ConfigFactory.load().getConfig("xio.proxyRouteTemplate"));
    ProxyRouteConfig subject = ProxyRouteConfig.newBuilder(fallbackObject).build();

    assertNotNull(subject);
  }
}
