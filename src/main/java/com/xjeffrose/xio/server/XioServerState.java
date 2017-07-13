package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.core.ChannelStatistics;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Getter;
import lombok.Setter;
import java.util.function.Function;

public class XioServerState {

  @Getter
  private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

  @Getter
  private final ChannelStatistics channelStatistics;


  @Getter
  @Setter
  private Function<Boolean, ChannelHandler> tracingHandler;
  //private ChannelHandler tracingHandler = null;

  public XioServerState(Config config) {
    channelStatistics = new ChannelStatistics(allChannels);
    tracingHandler = (b) -> null;
  }

  static public XioServerState fromConfig(String key, Config config) {
    return new XioServerState(config.getConfig(key));
  }

  static public XioServerState fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

}
