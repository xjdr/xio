package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ZkClient;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class XioServerState {

  private final ZkClient zkClient;
  private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
  private final ChannelStatistics channelStatistics;

  public XioServerState(ZkClient zkClient, ChannelStatistics channelStatistics) {
    this.zkClient = zkClient;
    this.channelStatistics = channelStatistics;
  }

  public XioServerState(Config config) {
    zkClient = new ZkClient(config.getString("settings.zookeeperCluster"));
    channelStatistics = new ChannelStatistics(allChannels);
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

}
