package com.xjeffrose.xio.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationConfig {

  private final Config config;

  @Getter
  private final String name;

  public ApplicationConfig(Config config) {
    this.config = config;
    name = config.getString("name");
  }

  public Config getServer(String server) {
    try {
      return config.getConfig("servers").getConfig(server);
    } catch (ConfigException e) {
      String servers = config
        .getConfig("servers")
        .root()
        .entrySet()
        .stream()
        .map( i -> i.getKey() )
        .collect(java.util.stream.Collectors.joining(", "));
      log.error("Invalid server '{}', available servers: {}", server, servers);
      throw e;
    }
  }

  public Config settings() {
    return config.getConfig("settings");
  }

}
