package com.xjeffrose.xio.client;

import com.google.common.base.Preconditions;
import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

@Log4j
@Accessors(fluent = true)
public class XioClientBootstrap {

  private final Bootstrap bootstrap;
  @Setter
  private InetSocketAddress address;
  @Setter
  private Distributor distributor;
  @Setter
  private boolean ssl;
  @Setter
  private Protocol proto;
  @Setter
  private Supplier<ChannelHandler> applicationProtocol;
  @Setter
  private ChannelHandler handler;
  private ChannelConfiguration channelConfig;

  public XioClientBootstrap(ChannelConfiguration channelConfig) {
    this.channelConfig = channelConfig;
    bootstrap = buildBootstrap();
  }

  public XioClientBootstrap(EventLoopGroup group) {
    this(ChannelConfiguration.clientConfig(group));
  }

  private ChannelInitializer<Channel> buildInitializer() {
    if (proto != null && (proto == Protocol.HTTP || proto == Protocol.HTTPS)) {
      return new DefaultChannelInitializer(handler, ssl);
    } else if (applicationProtocol != null) {
      ChannelInitializer<Channel> result = new DefaultChannelInitializer(handler, ssl) {
        @Override
        public ChannelHandler protocolHandler() {
          return applicationProtocol.get();
        }
      };
      return result;
    } else {
      throw new RuntimeException("Cannot build initializer, specify either protocol or applicationProtocol");
    }
  }

  public Bootstrap buildBootstrap() {
    return new Bootstrap()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
      .option(ChannelOption.SO_REUSEADDR, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
      .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
      .option(ChannelOption.TCP_NODELAY, true)
      .group(channelConfig.workerGroup())
      .channel(channelConfig.channel());
  }

  public XioClient build() {
    Preconditions.checkNotNull(handler);
    bootstrap.handler(buildInitializer());
    if (address != null) {
      bootstrap.remoteAddress(address);
      return new SingleNodeClient(address, bootstrap);
    } else if (distributor != null) {
      return new MultiNodeClient(distributor, bootstrap);
    } else {
      throw new RuntimeException("Cannot build XioClient, specify either address or distributor");
    }
  }

  public Bootstrap getBootstrap() {
    return bootstrap;
  }
}
