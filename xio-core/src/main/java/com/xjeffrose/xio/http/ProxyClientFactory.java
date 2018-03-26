package com.xjeffrose.xio.http;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.ChannelHandlerContext;

/** Generates an http proxy Client objects */
public class ProxyClientFactory extends ClientFactory {

  // TODO(CK): Client pool
  // http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/ArrayListMultimap.html
  // ListMultiMap<String, Client> pool

  /*
  private final Supplier<ChannelHandler> appHandler;

  public ClientFactory(ClientState state, Supplier<ChannelHandler> appHandler) {
    this.appHandler = appHandler;
  }
  */
  private final ApplicationState state;

  public ProxyClientFactory(ApplicationState state) {
    this.state = state;
  }

  @Override
  public Client createClient(ChannelHandlerContext ctx, ClientConfig config, XioTracing tracing) {
    ClientState clientState = state.createClientState(channelConfig(ctx), config);
    // TODO(CK): this handler should be notifying a connection pool on release
    return new Client(clientState, () -> new ProxyBackendHandler(ctx), tracing);
  }
}
