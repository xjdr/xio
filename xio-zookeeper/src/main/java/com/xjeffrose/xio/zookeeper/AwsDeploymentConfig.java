package com.xjeffrose.xio.zookeeper;

import com.typesafe.config.Config;
import lombok.Getter;

@Getter
public class AwsDeploymentConfig {
  private final ExhibitorConfig exhibitorConfig;
  private final String ipUrl;
  private final String identityUrl;
  private final ZookeeperConfig zookeeperConfig;

  public AwsDeploymentConfig(Config config) {
    this.exhibitorConfig = new ExhibitorConfig(config.getConfig("exhibitor"));
    this.ipUrl = config.getString("ipUrl");
    this.identityUrl = config.getString("identityUrl");
    this.zookeeperConfig = new ZookeeperConfig(config.getConfig("zookeeper"));
  }
}
