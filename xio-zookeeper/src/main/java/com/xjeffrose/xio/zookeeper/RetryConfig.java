package com.xjeffrose.xio.zookeeper;

import com.typesafe.config.Config;
import lombok.Getter;

@Getter
public class RetryConfig {
  private final int baseSleepTimeMs;
  private final int maxRetries;

  public RetryConfig(Config config) {
    this.baseSleepTimeMs = (int) config.getDuration("baseSleepTime").toMillis();
    this.maxRetries = (int) config.getInt("maxRetries");
  }
}
