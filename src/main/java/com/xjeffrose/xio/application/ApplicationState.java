package com.xjeffrose.xio.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.bootstrap.ServerChannelConfiguration;
import lombok.Getter;

public class ApplicationState {

  @Getter
  private final ZkClient zkClient;

  @Getter
  private final ServerChannelConfiguration channelConfiguration;

  public ApplicationState(Config config) {
    zkClient = new ZkClient(config.getString("settings.zookeeperCluster"));
    channelConfiguration = ChannelConfiguration.serverConfig(
      config.getInt("settings.bossThreads"),
      config.getString("settings.bossNameFormat"),
      config.getInt("settings.workerThreads"),
      config.getString("settings.workerNameFormat")
    );
  }

  static public ApplicationState fromConfig(String key, Config config) {
    return new ApplicationState(config.getConfig(key));
  }

  static public ApplicationState fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }
}
