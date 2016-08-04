package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ZkClient;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class XioServerState {

  private final ZkClient zkClient;
  private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
  private final ChannelStatistics channelStatistics;
  private final ChannelConfiguration channelConfiguration;

  public XioServerState(ZkClient zkClient, ChannelStatistics channelStatistics) {
    this.zkClient = zkClient;
    this.channelStatistics = channelStatistics;
    channelConfiguration = null;
  }

  public XioServerState(Config config) {
    zkClient = new ZkClient(config.getString("settings.zookeeperCluster"));
    channelStatistics = new ChannelStatistics(allChannels);
    channelConfiguration = ChannelConfiguration.serverConfig(config.getInt("settings.bossThreads"), config.getInt("settings.workerThreads"));
  }

  static public XioServerState fromConfig(String key, Config config) {
    return new XioServerState(config.getConfig(key));
  }

  static public XioServerState fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

  public ZkClient zkClient() {
    return zkClient;
  }

  public ChannelStatistics channelStatistics() {
    return channelStatistics;
  }

  public ChannelConfiguration channelConfiguration() {
    return channelConfiguration;
  }

}
