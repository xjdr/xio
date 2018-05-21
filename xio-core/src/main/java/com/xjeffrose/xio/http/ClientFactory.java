package com.xjeffrose.xio.http;

import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import java.util.Optional;
import lombok.Getter;

// TODO(CK): only use case is currently for proxy clients, establish more use cases.
/** Base class for generating http clients */
public abstract class ClientFactory {

  @Getter private final XioTracing tracing;

  private static final AttributeKey<Client> CLIENT_KEY =
      AttributeKey.newInstance("xio_http_client_key");

  public ClientFactory(XioTracing tracing) {
    this.tracing = tracing;
  }

  public ClientChannelConfiguration channelConfig(ChannelHandlerContext ctx) {
    return ChannelConfiguration.clientConfig(ctx.channel().eventLoop());
  }

  public abstract Client createClient(ChannelHandlerContext ctx, ClientConfig config);

  protected Optional<Client> getHandlerClient(ChannelHandlerContext ctx) {
    return Optional.ofNullable(ctx.channel().attr(CLIENT_KEY).get());
  }

  public Client getClient(ChannelHandlerContext ctx, ClientConfig config) {
    return getHandlerClient(ctx)
        .orElseGet(
            () -> {
              Client client = createClient(ctx, config);
              updateChannelAttr(ctx, client);
              return client;
            });
  }

  public void updateChannelAttr(ChannelHandlerContext ctx, Client client) {
    ctx.channel().attr(CLIENT_KEY).set(client);
  }
}
