package com.xjeffrose.xio.server;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

// TODO(CK): rename this to ServerLimits
public class XioServerLimits {

  private final int maxConnections;
  private final int maxFrameSize;
  private final Duration maxReadIdleTime;
  private final Duration maxWriteIdleTime;
  private final Duration maxAllIdleTime;
  private final double globalHardReqPerSec;
  private final double globalSoftReqPerSec;
  private final int rateLimiterPoolSize;
  private final double softReqPerSec;
  private final double hardReqPerSec;
  private final ImmutableMap<String, List<Double>> clientRateLimitOverride;

  public XioServerLimits(Config config) {
    maxConnections = config.getInt("maxConnections");
    maxFrameSize = config.getInt("maxFrameSize");
    maxReadIdleTime = config.getDuration("maxReadIdleTime");
    maxWriteIdleTime = config.getDuration("maxWriteIdleTime");
    maxAllIdleTime = config.getDuration("maxAllIdleTime");
    globalHardReqPerSec = config.getDouble("globalHardReqPerSec");
    globalSoftReqPerSec = config.getDouble("globalSoftReqPerSec");
    rateLimiterPoolSize = config.getInt("rateLimiterPoolSize");
    softReqPerSec = config.getDouble("softReqPerSec");
    hardReqPerSec = config.getDouble("hardReqPerSec");
    clientRateLimitOverride = createClientRateLimitOverride(config);
  }

  private static ImmutableMap<String, List<Double>> createClientRateLimitOverride(Config config) {
    ImmutableMap.Builder<String, List<Double>> builder = ImmutableMap.builder();
    config
        .getObjectList("reqPerSecondOverride")
        .forEach(
            each ->
                each.forEach(
                    (key, value) -> {
                      builder.put(
                          key,
                          Arrays.stream(value.unwrapped().toString().split(":"))
                              .map(Double::parseDouble)
                              .collect(Collectors.toList()));
                    }));
    return builder.build();
  }

  public int maxConnections() {
    return maxConnections;
  }

  public int maxFrameSize() {
    return maxFrameSize;
  }

  public Duration maxReadIdleTime() {
    return maxReadIdleTime;
  }

  public Duration maxWriteIdleTime() {
    return maxWriteIdleTime;
  }

  public Duration maxAllIdleTime() {
    return maxAllIdleTime;
  }

  public double getGlobalHardReqPerSec() {
    return globalHardReqPerSec;
  }

  public double getGlobalSoftReqPerSec() {
    return globalSoftReqPerSec;
  }

  public int rateLimiterPoolSize() {
    return rateLimiterPoolSize;
  }

  public double softReqPerSec() {
    return softReqPerSec;
  }

  public double hardReqPerSec() {
    return hardReqPerSec;
  }

  public ImmutableMap<String, List<Double>> getClientRateLimitOverride() {
    return clientRateLimitOverride;
  }
}
