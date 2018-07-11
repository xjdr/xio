package com.xjeffrose.xio.http;

import static com.xjeffrose.xio.helpers.TlsHelper.getKeyManagers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.helpers.TlsHelper;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import java.io.File;
import java.util.Arrays;
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
    ClientConfig clientConfig = new ClientConfig(config);
    ClientState clientState = new ClientState(channelConfiguration, clientConfig);

    ClientChannelInitializer clientChannelInit =
        new ClientChannelInitializer(clientState, () -> new HollowChannelHandler(), null);
    return new ClientConnectionManager(clientState, clientChannelInit);
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.close();
    }
  }

  @Test
  public void testInitialConditions() {
    subject = subjectFactory(true);
    assertEquals(subject.connectionState(), ClientConnectionState.NOT_CONNECTED);
  }

  @Test
  public void testConnectingFailingConnection() throws Exception {
    subject = subjectFactory(false);
    // don't set up fake origin backend server so we can connect to it
    Future<Void> connectionResult = subject.connect();
    assertEquals(ClientConnectionState.CONNECTING, subject.connectionState());
    connectionResult.awaitUninterruptibly(5000);
    // this is best effort, sometimes it takes like 60 seconds for the connection to fail (i'm trying to
    // write connect to 127.0.0.0:8888 which should fail. I make this best effort because i dont want to
    // set the timer to 60 seconds)
    if (connectionResult.isDone()) {
      assertEquals(ClientConnectionState.CLOSED_CONNECTION, subject.connectionState());
    }
  }

  @Test
  public void testConnectingSuccessfulConnection() throws Exception {
    // set up fake origin backend server so we can connect to it, we connect to port 8888
    // the outbound 8888 is specified in the ClientConnectionManagerIntegrationTest.conf
    TlsConfig tlsConfig =
        TlsConfig.fromConfig("xio.h2BackendServer.settings.tls", ConfigFactory.load());
    server = OkHttpUnsafe.getSslMockWebServer(TlsHelper.getKeyManagers(tlsConfig));
    server.setProtocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1));
    // tell the server to bind to 8888
    server.start(8888);

    subject = subjectFactory(true);
    Future<Void> connectionResult = subject.connect();
    assertEquals(ClientConnectionState.CONNECTING, subject.connectionState());
    connectionResult.await(5000);
    assertEquals(ClientConnectionState.CONNECTED, subject.connectionState());
  }
}
