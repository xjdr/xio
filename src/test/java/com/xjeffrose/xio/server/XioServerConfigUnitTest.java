package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;

public class XioServerConfigUnitTest extends Assert {

  @Test
  public void testLoadWithConfig() {
    Config config = ConfigFactory.load();
    XioServerConfig serverConfig = new XioServerConfig(config.getConfigList("xio.servers").get(0));
    assertEquals(new InetSocketAddress("127.0.0.1", 0), serverConfig.getBindAddress());
    assertEquals("example", serverConfig.getName());
  }
}
