package com.xjeffrose.xio.http;

import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

// TODO(CK): This needs to replace the ClientState in client
public class ClientState {

  public final ClientChannelConfiguration channelConfig;
  public final ClientConfig config;
  public final InetSocketAddress remote;
  public final SslContext sslContext;
  public final Supplier<ChannelHandler> tracingHandler;

  private static SslContext sslContext(boolean enableTls, ClientConfig clientConfig) {
    if (enableTls) {
      return SslContextFactory.buildClientContext(clientConfig.getTls());
    } else {
      return null;
    }
  }

  public ClientState(
      ClientChannelConfiguration channelConfig,
      ClientConfig config,
      InetSocketAddress remote,
      SslContext sslContext,
      Supplier<ChannelHandler> tracingHandler) {
    this.channelConfig = channelConfig;
    this.config = config;
    this.remote = remote;
    this.sslContext = sslContext;
    this.tracingHandler = tracingHandler;
  }

  public ClientState(
      ClientChannelConfiguration channelConfig,
      ClientConfig config,
      InetSocketAddress remote,
      boolean enableTls,
      Supplier<ChannelHandler> tracingHandler) {
    this(channelConfig, config, remote, sslContext(enableTls, config), tracingHandler);
  }

  public ClientState(
      ClientChannelConfiguration channelConfig,
      ClientConfig config,
      Supplier<ChannelHandler> tracingHandler) {
    this.channelConfig = channelConfig;
    this.config = config;
    this.remote = config.remote();
    this.sslContext = sslContext(config.isTlsEnabled(), config);
    this.tracingHandler = tracingHandler;
  }
}
