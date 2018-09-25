package com.xjeffrose.xio.http;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.helpers.TlsHelper;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import com.xjeffrose.xio.tls.TlsConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import okhttp3.Protocol;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class ClientConnectionManagerIntegrationTest extends Assert {

  private class HollowChannelHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {}
  }

  private ClientConnectionManager subject;
  private MockWebServer server;

  private ClientConnectionManager subjectFactory(boolean shouldSucceed) {
    ClientChannelConfiguration channelConfiguration =
        new ClientChannelConfiguration(new NioEventLoopGroup(), NioSocketChannel.class);

    File configFile;
    if (shouldSucceed) {
      configFile = new File("src/test/resources/ClientConnectionManagerTestSucceed.conf");
    } else {
      configFile = new File("src/test/resources/ClientConnectionManagerTestFail.conf");
    }
    Config config = ConfigFactory.parseFile(configFile);
    ClientConfig clientConfig = ClientConfig.from(config);
    ClientState clientState = new ClientState(channelConfiguration, clientConfig);

    ClientChannelInitializer clientChannelInit =
        new ClientChannelInitializer(clientState, () -> new HollowChannelHandler(), null);
    return new ClientConnectionManager(clientState, clientChannelInit);
  }

  @After
  public void tearDown() throws Exception {
    subject = null;
  }

  @Test
  public void testInitialConditions() {
    subject = subjectFactory(true);
    assertEquals(subject.connectionState(), ClientConnectionState.NOT_CONNECTED);
  }

  @Test
  public void testConnectingSuccessfulConnection() throws Exception {
    // set up fake origin backend server so we can connect to it, we connect to port 8888
    // the outbound 8888 is specified in the ClientConnectionManagerIntegrationTest.conf
    TlsConfig tlsConfig =
        TlsConfig.builderFrom(ConfigFactory.load().getConfig("xio.h2BackendServer.settings.tls"))
            .build();
    server = OkHttpUnsafe.getSslMockWebServer(TlsHelper.getKeyManagers(tlsConfig));
    server.setProtocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1));
    // tell the server to bind to 8888
    server.start(8888);

    subject = subjectFactory(true);
    Future<Void> connectionResult = subject.connect();
    assertEquals(ClientConnectionState.CONNECTING, subject.connectionState());
    Thread.sleep(100); // todo: (WK) do something smarter
    try {
      connectionResult.get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      System.out.println("Connection exception = " + e.toString());
    } finally {
      assertEquals(ClientConnectionState.CONNECTED, subject.connectionState());
      server.close();
    }
  }

  @Test
  public void testConnectingFailingConnection() throws Exception {
    subject = subjectFactory(false);
    // don't set up fake origin backend server so we can connect to it
    Future<Void> connectionResult = subject.connect();
    assertEquals(ClientConnectionState.CONNECTING, subject.connectionState());
    Thread.sleep(100); // todo: (WK) do something smarter
    try {
      connectionResult.get(30, TimeUnit.SECONDS);
    } catch (Exception ignored) {
    } finally {
      assertEquals(ClientConnectionState.CLOSED_CONNECTION, subject.connectionState());
    }
  }
}
