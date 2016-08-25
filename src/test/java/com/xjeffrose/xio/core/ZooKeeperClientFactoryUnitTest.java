package com.xjeffrose.xio.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

public class ZooKeeperClientFactoryUnitTest extends Assert {

  @Test
  public void testConfigs() {
    Config config = ConfigFactory.load().getConfig("xio.testApplication.settings");

    for (ZooKeeperClientFactory.ClientRetryPolicy policy : ZooKeeperClientFactory.ClientRetryPolicy.values()) {
      Config override = ConfigFactory.parseString("zookeeper { client { retry { policy = " + policy.name() + "} } }");
      ZooKeeperClientFactory factory = new ZooKeeperClientFactory(override.withFallback(config).getConfig("zookeeper"));

      factory.newClient();
      assertTrue(true); // no exception thrown
    }
  }

  @Test(expected=RuntimeException.class)
  public void testBadConfig() {
    Config config = ConfigFactory.load().getConfig("xio.testApplication.settings");

    Config override = ConfigFactory.parseString("zookeeper { client { retry { policy = BadPolicy } } }");
    ZooKeeperClientFactory factory = new ZooKeeperClientFactory(override.withFallback(config).getConfig("zookeeper"));

    factory.newClient();
  }

}
