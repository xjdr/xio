package com.xjeffrose.xio.http;

import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;

// TODO(CK): This needs to replace the ClientState in client
public class ClientState {

  public final ClientChannelConfiguration channelConfig;
  public final ClientConfig config;
  public final InetSocketAddress remote;
  public final SslContext sslContext;
  public final boolean idleTimeoutEnabled;
  public final int idleTimeoutDuration;

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
      SslContext sslContext) {
    this.channelConfig = channelConfig;
    this.config = config;
    this.remote = remote;
    this.sslContext = sslContext;
    idleTimeoutEnabled = config.getIdleTimeoutConfig().enabled;
    idleTimeoutDuration = config.getIdleTimeoutConfig().duration;
  }

  public ClientState(
      ClientChannelConfiguration channelConfig,
      ClientConfig config,
      InetSocketAddress remote,
      boolean enableTls) {
    this(channelConfig, config, remote, sslContext(enableTls, config));
  }

  public ClientState(ClientChannelConfiguration channelConfig, ClientConfig config) {
    this(channelConfig, config, config.remote(), sslContext(config.isTlsEnabled(), config));
  }
}
