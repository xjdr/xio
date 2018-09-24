package com.xjeffrose.xio.zookeeper;

import com.typesafe.config.Config;
import lombok.Getter;

@Getter
public class ZookeeperConfig {
  private final String membershipPath;
  private final RetryConfig retryConfig;

  public ZookeeperConfig(Config config) {
    this.membershipPath = config.getString("membershipPath");
    this.retryConfig = new RetryConfig(config.getConfig("retry"));
  }
}
