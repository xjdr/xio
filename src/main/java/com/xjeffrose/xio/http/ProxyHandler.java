package com.xjeffrose.xio.http;

import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import java.net.InetSocketAddress;

public class ProxyHandler implements PipelineRequestHandler {

  private static final AttributeKey<Client> CLIENT_KEY =
      AttributeKey.newInstance("xio_proxy_client_key");

  private static void setClient(ChannelHandlerContext ctx, Client client) {
    ctx.channel().attr(CLIENT_KEY).set(client);
  }

  private Client getClient(ChannelHandlerContext ctx) {
    Client client = ctx.channel().attr(CLIENT_KEY).get();
    if (client == null) {
      ClientState state =
          new ClientState(channelConfig(ctx), clientConfig, remote, enableTls, () -> null);
      // TODO(CK): this handler should be notifying a connection pool on release
      client = new Client(state, () -> new ProxyBackendHandler(ctx));
      setClient(ctx, client);
    }
    return client;
  }

  final ClientConfig clientConfig;
  final InetSocketAddress remote;
  final boolean enableTls;

  private ClientChannelConfiguration channelConfig(ChannelHandlerContext ctx) {
    return ChannelConfiguration.clientConfig(ctx.channel().eventLoop());
  }

  // TODO(CK): this should really be requesting a client from a pool
  public ProxyHandler(ClientConfig clientConfig, ProxyConfig config) {
    this.clientConfig = clientConfig;
    this.remote = config.address;
    this.enableTls = config.needSSL;
  }

  public void handle(ChannelHandlerContext ctx, Request request, Route route) {

    /*
    Optional<String> path =
      route
      .groups(request.path)
      .entrySet()
      .stream()
      .filter(e -> e.getKey().equals("path"))
      .map(e -> e.getValue())
      .findFirst();

    payload.setUri(path.map(config.urlPath::concat).orElse(config.urlPath));

    payload.headers().set("Host", config.hostHeader);

    XioRequest request =
        HttpTracingState.hasSpan(ctx)
            ? new XioRequest(payload, HttpTracingState.getSpan(ctx).context())
            : new XioRequest(payload, null);
    */

    // 1) map the incoming request path to the outgoing request path
    // 2) set the outgoing request host
    // 3) set the tracing span (if there is one)

    getClient(ctx).write(request);
  }
}
