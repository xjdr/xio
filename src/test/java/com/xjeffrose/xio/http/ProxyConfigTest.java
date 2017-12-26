package com.xjeffrose.xio.http;

import org.junit.Assert;
import org.junit.Test;

public class ProxyConfigTest extends Assert {

  @Test
  public void testParseHttp() throws Exception {
    ProxyConfig config = ProxyConfig.parse("http://server.com/");

    assertEquals("server.com", config.address.getHostString());
    assertEquals(80, config.address.getPort());
    assertEquals("server.com", config.hostHeader);
    assertEquals("http://server.com/", config.url);
    assertEquals("/", config.urlPath);
    assertEquals(false, config.needSSL);
  }
}
