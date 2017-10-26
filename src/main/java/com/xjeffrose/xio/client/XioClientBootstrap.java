package com.xjeffrose.xio.client;

import com.google.common.base.Preconditions;
import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.client.loadbalancer.Distributor;
import com.xjeffrose.xio.client.loadbalancer.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslContext;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

@Slf4j
@Accessors(fluent = true)
public class XioClientBootstrap {

  private Bootstrap bootstrap;
  private ChannelConfiguration channelConfig;
  private ClientConfig config;
  @Setter
  private SslContext sslContext;
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
  private Supplier<ChannelHandler> tracingHandler;
  @Setter
  private ChannelHandler handler;
  @Setter
  boolean usePool;

  private XioClientBootstrap(XioClientBootstrap other) {
    this.address = other.address;
    this.distributor = other.distributor;
    this.ssl = other.ssl;
    this.proto = other.proto;
    this.applicationProtocol = other.applicationProtocol;
    this.tracingHandler = other.tracingHandler;
    this.handler = other.handler;
    this.usePool = other.usePool;
  }

  public XioClientBootstrap() {
    usePool = false;
    tracingHandler = () -> null;
  }

  public XioClientBootstrap(ChannelConfiguration channelConfig) {
    this();
    this.channelConfig(channelConfig);
  }

  public XioClientBootstrap(EventLoopGroup group) {
    this(ChannelConfiguration.clientConfig(group));
  }

  public XioClientBootstrap(ClientConfig config) {
    this();
    this.config = config;
    sslContext = SslContextFactory.buildClientContext(config.getTls());
  }

  public XioClientBootstrap channelConfig(ChannelConfiguration channelConfig) {
    this.channelConfig = channelConfig;
    bootstrap = buildBootstrap(channelConfig);
    return this;
  }

  private ChannelInitializer<Channel> buildInitializer() {
    if (proto != null && (proto == Protocol.HTTP || proto == Protocol.HTTPS)) {
      applicationProtocol = () -> new HttpClientCodec();
    } else if (applicationProtocol == null) {
      throw new RuntimeException("Cannot build initializer, specify either protocol or applicationProtocol");
    }

    if (ssl) {
      sslContext = SslContextFactory.buildServerContext(config.getTls());
    }

    return new DefaultChannelInitializer(address, handler, sslContext, applicationProtocol, tracingHandler);
  }

  public Bootstrap buildBootstrap(ChannelConfiguration channelConfig) {
    return new Bootstrap()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
      .option(ChannelOption.SO_REUSEADDR, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
      .option(ChannelOption.TCP_NODELAY, true)
      .group(channelConfig.workerGroup())
      .channel(channelConfig.channel());
  }

  public XioClient build() {
    Preconditions.checkNotNull(channelConfig, "channelConfig must be defined");
    Preconditions.checkNotNull(handler, "handler must be defined");
    bootstrap.handler(buildInitializer());
    if (address != null) {
      bootstrap.remoteAddress(address);
      if (usePool) {
        return new SingleNodeClient(address, bootstrap);
      } else {
        return new SingleUnpooledNodeClient(address, bootstrap);
      }
    } else if (distributor != null) {
      return new MultiNodeClient(distributor, bootstrap);
    } else {
      throw new RuntimeException("Cannot build XioClient, specify either address or distributor");
    }
  }

  public Bootstrap getBootstrap() {
    return bootstrap;
  }

  public XioClientBootstrap clone(ChannelConfiguration channelConfig) {
    XioClientBootstrap bs = new XioClientBootstrap(this);
    return bs.channelConfig(channelConfig);
  }

  public XioClientBootstrap clone(EventLoopGroup group) {
    return clone(ChannelConfiguration.clientConfig(group));
  }

}
