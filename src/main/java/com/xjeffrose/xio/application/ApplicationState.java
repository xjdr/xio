package com.xjeffrose.xio.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.bootstrap.ServerChannelConfiguration;
import com.xjeffrose.xio.filter.Http1FilterConfig;
import com.xjeffrose.xio.filter.IpFilterConfig;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicReference;

public class ApplicationState {

  @Getter
  private final ZkClient zkClient;

  @Getter
  private final ServerChannelConfiguration channelConfiguration;

  private final AtomicReference<IpFilterConfig> ipFilterConfig;

  private final AtomicReference<Http1FilterConfig> http1FilterConfig;

  public ApplicationState(Config config) {
    zkClient = new ZkClient(config.getString("settings.zookeeperCluster"));
    channelConfiguration = ChannelConfiguration.serverConfig(
      config.getInt("settings.bossThreads"),
      config.getString("settings.bossNameFormat"),
      config.getInt("settings.workerThreads"),
      config.getString("settings.workerNameFormat")
    );

    String ipFilterPath = config.getString("settings.configurationManager.ipFilter.path");
    ipFilterConfig = new AtomicReference<IpFilterConfig>(new IpFilterConfig());
    zkClient.registerUpdater(new IpFilterConfig.Updater(ipFilterPath, this::setIpFilterConfig));

    String http1FilterPath = config.getString("settings.configurationManager.http1Filter.path");
    http1FilterConfig = new AtomicReference<Http1FilterConfig>(new Http1FilterConfig());
    zkClient.registerUpdater(new Http1FilterConfig.Updater(http1FilterPath, this::setHttp1FilterConfig));

  }

  static public ApplicationState fromConfig(String key, Config config) {
    return new ApplicationState(config.getConfig(key));
  }

  static public ApplicationState fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

  public IpFilterConfig getIpFilterConfig() {
    return ipFilterConfig.get();
  }

  public void setIpFilterConfig(IpFilterConfig newConfig) {
    ipFilterConfig.set(newConfig);
  }

  public Http1FilterConfig getHttp1FilterConfig() {
    return http1FilterConfig.get();
  }

  public void setHttp1FilterConfig(Http1FilterConfig newConfig) {
    http1FilterConfig.set(newConfig);
  }

}
