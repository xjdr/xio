package com.xjeffrose.xio.mux;

import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import java.net.InetSocketAddress;

public class SocketConnector extends Connector {

  protected final ClientChannelConfiguration channelConfig;

  @Override
  protected EventLoopGroup group() {
    return channelConfig.workerGroup();
  }

  @Override
  protected Class<? extends Channel> channel() {
    return channelConfig.channel();
  }

  @Override
  protected Bootstrap configure(Bootstrap bootstrap) {
    return bootstrap
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.TCP_NODELAY, true);
  }

  public SocketConnector(InetSocketAddress address, ClientChannelConfiguration channelConfig) {
    super(address);
    this.channelConfig = channelConfig;
  }
}
