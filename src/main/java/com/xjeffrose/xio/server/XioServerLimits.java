package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import java.time.Duration;

public class XioServerLimits {

  private final int maxConnections;
  private final int maxFrameSize;
  private final Duration maxReadIdleTime;
  private final Duration maxWriteIdleTime;
  private final Duration maxAllIdleTime;

  public XioServerLimits(Config config) {
    maxConnections = config.getInt("maxConnections");
    maxFrameSize = config.getInt("maxFrameSize");
    maxReadIdleTime = config.getDuration("maxReadIdleTime");
    maxWriteIdleTime = config.getDuration("maxWriteIdleTime");
    maxAllIdleTime = config.getDuration("maxAllIdleTime");
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

}
