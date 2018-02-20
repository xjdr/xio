package com.xjeffrose.xio.http;

import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

// TODO(CK): only use case is currently for proxy clients, establish more use cases.
/** Base class for generating http clients */
public abstract class ClientFactory {

  private static final AttributeKey<Client> CLIENT_KEY =
      AttributeKey.newInstance("xio_http_client_key");

  public ClientChannelConfiguration channelConfig(ChannelHandlerContext ctx) {
    return ChannelConfiguration.clientConfig(ctx.channel().eventLoop());
  }

  public abstract Client createClient(ChannelHandlerContext ctx, ClientConfig config);

  public Client getClient(ChannelHandlerContext ctx, ClientConfig config) {
    Client client = ctx.channel().attr(CLIENT_KEY).get();
    if (client == null) {
      client = createClient(ctx, config);
      ctx.channel().attr(CLIENT_KEY).set(client);
    }
    return client;
  }
}
