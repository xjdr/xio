package com.xjeffrose.xio.http;

import com.google.common.annotations.VisibleForTesting;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;

// TODO(CK): only use case is currently for proxy clients, establish more use cases.
/** Base class for generating http clients */
public abstract class ClientFactory {

  @Getter private final XioTracing tracing;

  private static final AttributeKey<Map<InetSocketAddress, Client>> CLIENT_KEY =
      AttributeKey.newInstance("xio_http_client_key");

  public ClientFactory(XioTracing tracing) {
    this.tracing = tracing;
  }

  public ClientChannelConfiguration channelConfig(ChannelHandlerContext ctx) {
    return ChannelConfiguration.clientConfig(ctx.channel().eventLoop());
  }

  public abstract Client createClient(ChannelHandlerContext ctx, ClientConfig config);

  protected Optional<Client> getHandlerClient(
      ChannelHandlerContext ctx, InetSocketAddress address) {
    return Optional.ofNullable(getClientMap(ctx).get(address))
        .flatMap(
            it -> {
              if (it.isReusable()) {
                return Optional.of(it);
              } else {
                getClientMap(ctx).remove(address);
                return Optional.empty();
              }
            });
  }

  public Client getClient(ChannelHandlerContext ctx, ClientConfig config) {
    return getHandlerClient(ctx, config.remote())
        .orElseGet(
            () -> {
              Client client = createClient(ctx, config);
              updateChannelAttr(ctx, config.remote(), client);
              return client;
            });
  }

  @VisibleForTesting
  Map<InetSocketAddress, Client> getClientMap(ChannelHandlerContext ctx) {
    Map<InetSocketAddress, Client> map = ctx.channel().attr(CLIENT_KEY).get();
    if (map == null) {
      map = new HashMap<>();
      ctx.channel().attr(CLIENT_KEY).set(map);
    }
    return map;
  }

  public void updateChannelAttr(
      ChannelHandlerContext ctx, InetSocketAddress remoteAddress, Client client) {
    getClientMap(ctx).put(remoteAddress, client);
  }
}
