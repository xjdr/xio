package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.core.ChannelStatistics;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Getter;

public class XioServerState {

  @Getter
  private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

  @Getter
  private final ChannelStatistics channelStatistics;

  public XioServerState(Config config) {
    channelStatistics = new ChannelStatistics(allChannels);
  }

  static public XioServerState fromConfig(String key, Config config) {
    return new XioServerState(config.getConfig(key));
  }

  static public XioServerState fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

}
