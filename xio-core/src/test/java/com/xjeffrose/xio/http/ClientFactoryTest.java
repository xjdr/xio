package com.xjeffrose.xio.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class ClientFactoryTest extends Assert {

  @Test
  public void getHandlerClientClosed() throws Exception {
    // given a context
    EmbeddedChannel embeddedChannel =
        new EmbeddedChannel(
            new SimpleChannelInboundHandler<Object>() {
              @Override
              protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {}
            });
    ChannelHandlerContext ctx = embeddedChannel.pipeline().firstContext();

    // given a client factory
    ClientFactory factory =
        new ClientFactory(mock(XioTracing.class)) {
          @Override
          public Client createClient(ChannelHandlerContext ctx, ClientConfig config) {
            return mock(Client.class);
          }
        };

    // given a closed client for a given remote address
    InetSocketAddress remoteAddress = new InetSocketAddress("remote", 80);
    Client closeClient = mock(Client.class);
    when(closeClient.isReusable()).thenReturn(false);
    factory.getClientMap(ctx).put(remoteAddress, closeClient);

    // when a client is requested for the remote address
    Optional<Client> client = factory.getHandlerClient(ctx, remoteAddress);

    // then the factory is forced to create a new client
    assertFalse(client.isPresent());

    // and the ctx no longer contains the closed client
    assertTrue(factory.getClientMap(ctx).isEmpty());
  }
}
