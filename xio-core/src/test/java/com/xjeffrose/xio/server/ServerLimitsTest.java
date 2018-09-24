package com.xjeffrose.xio.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ServerLimitsTest extends Assert {

  @Test
  public void testReadGoodValues() {
    //when limits are read
    ServerLimits limits = new ServerLimits(ConfigFactory.parseResources("limits.conf"));

    // then the values are correct
    assertEquals(501, limits.maxConnections());
    assertEquals(9601, limits.maxFrameSize());
    assertEquals(Duration.ofSeconds(61), limits.maxReadIdleTime());
    assertEquals(Duration.ofSeconds(62), limits.maxWriteIdleTime());
    assertEquals(Duration.ofSeconds(63), limits.maxAllIdleTime());
    assertEquals(10001D, limits.globalHardReqPerSec(), 0);
    assertEquals(10000D, limits.globalSoftReqPerSec(), 0);
    assertEquals(25, limits.rateLimiterPoolSize());
    assertEquals(501.0D, limits.softReqPerSec(), 0);
    assertEquals(551.0D, limits.hardReqPerSec(), 0);
    ImmutableMap<String, List<Double>> crlo = limits.clientRateLimitOverride();
    assertEquals(2, crlo.size());
    assertEquals(Lists.newArrayList(500D, 550D), crlo.get("1.2.3.4"));
    assertEquals(Lists.newArrayList(1000D, 1500D), crlo.get("2.2.2.2"));
  }

  @Test(expected = RuntimeException.class)
  public void readBadValues() {
    // when limits are read
    // and maxConnections is greater than ServerLimits.MAX_CONNECTION_LIMIT
    new ServerLimits(ConfigFactory.parseResources("limits_over_max_limit.conf"));
    // then fail fast
  }
}
