package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.SSL.SslContextFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslContext;
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

  @Getter
  private final SslContext sslContext;

  public XioServerState(XioServerConfig config) {
    channelStatistics = new ChannelStatistics(allChannels);
    tracingHandler = (b) -> null;
    sslContext = SslContextFactory.buildServerContext(config.getTls());
  }
}
