package com.xjeffrose.xio.application;

import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

public class ApplicationConfigTest extends Assert {

  @Test
  public void testFromConfig() {
    ApplicationConfig config =
        ApplicationConfig.fromConfig("xio.exampleApplication", ConfigFactory.load());

    assertEquals("example application", config.getName());
    assertEquals(5, config.getBossThreads());
    assertEquals("xio-application-boss-%d", config.getBossNameFormat());
    assertEquals(10, config.getWorkerThreads());
    assertEquals("xio-application-worker-%d", config.getWorkerNameFormat());
    assertEquals("localhost:2181", config.getZookeeperCluster());
    assertEquals("/xio/ipFilterRules", config.getIpFilterPath());
    assertEquals("/xio/http1FilterRules", config.getHttp1FilterPath());
  }
}
