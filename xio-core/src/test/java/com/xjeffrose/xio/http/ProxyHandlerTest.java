package com.xjeffrose.xio.http;

import static org.mockito.Mockito.when;

import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.SocketAddressHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProxyHandlerTest extends Assert {

  private class TestHandler extends SimpleChannelInboundHandler {

    private ProxyHandler proxyHandler;

    public TestHandler(ProxyHandler proxyHandler) {
      this.proxyHandler = proxyHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
      proxyHandler.handle(ctx, request, null);
    }
  }

  ProxyHandler subject;
  DefaultFullRequest request;

  ClientFactory factory;
  @Mock ProxyRouteConfig config;
  @Mock SocketAddressHelper addressHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testNoClients() {
    request = RequestBuilders.newGet("https://www.google.com/").build();
    when(config.clientConfigs()).thenReturn(new ArrayList<ClientConfig>());
    subject = new ProxyHandler(factory, config, addressHelper);

    EmbeddedChannel channel = new EmbeddedChannel(new TestHandler(subject));
    channel.writeInbound("doesn't matter");
    assertTrue(channel.finish());

    DefaultFullResponse result = channel.readOutbound();
    assertEquals(result.status().code(), 404);
  }
}
