package com.xjeffrose.xio.http;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.client.ClientConfig;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/** Generates an http proxy Client objects */
@Slf4j
public class ProxyClientFactory extends ClientFactory {

  private final ClientPool clientPool;

  public ProxyClientFactory(ApplicationState state) {
    super(state.tracing());
    this.clientPool = new ClientPool(state.config().getClientPoolSize());
  }

  @Override
  public Client createClient(ChannelHandlerContext ctx, ClientConfig config) {
    ClientState clientState = new ClientState(channelConfig(ctx), config);
    ClientChannelInitializer clientChannelInit =
        new ClientChannelInitializer(clientState, () -> new ProxyBackendHandler(ctx), getTracing());
    ClientConnectionManager connManager =
        new ClientConnectionManager(clientState, clientChannelInit);
    Client client = new Client(clientState, connManager);
    ctx.channel().closeFuture().addListener(f -> clientPool.release(client));
    log.debug("creating client");
    return client;
  }

  @Override
  public Client getClient(ChannelHandlerContext ctx, ClientConfig config) {
    Client client =
        getHandlerClient(ctx)
            .orElseGet(() -> clientPool.acquire(ctx, config, () -> createClient(ctx, config)));
    updateChannelAttr(ctx, client);
    return client;
  }
}
