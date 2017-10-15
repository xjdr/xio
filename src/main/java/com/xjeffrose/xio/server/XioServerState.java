package com.xjeffrose.xio.server;

import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.core.ChannelStatistics;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;

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
