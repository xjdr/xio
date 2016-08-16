package com.xjeffrose.xio.application;

import com.typesafe.config.Config;
import lombok.Getter;

public class ApplicationConfig {

  private final Config config;

  @Getter
  private final String name;

  public ApplicationConfig(Config config) {
    this.config = config;
    name = config.getString("name");
  }

  public Config getServer(String server) {
    return config.getConfig("servers").getConfig(server);
  }

}
