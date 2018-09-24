package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.http.test_helpers.GrpcClient;
import com.xjeffrose.xio.http.test_helpers.GrpcServer;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import com.xjeffrose.xio.tracing.XioTracing;
import helloworld.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

@Slf4j
public class GrpcProxyTest extends Assert {

  private EventLoopGroup group;

  @Rule public TestName testName = new TestName();

  @Before
  public void setUp() {
    group = new NioEventLoopGroup(2);
    log.debug("Test: " + testName.getMethodName());
  }

  @After
  public void tearDown() {
    group.shutdownGracefully(0, 1000, TimeUnit.MILLISECONDS).syncUninterruptibly();
  }

  @Test
  public void sanityCheck() throws Exception {
    GrpcServer server = GrpcServer.run();
    GrpcClient client = GrpcClient.run(server.getPort());

    HelloReply response = client.greet("world");

    assertEquals("Expected Response", "Hello world", response.getMessage());

    client.shutdown();
    server.stop();
    server.blockUntilShutdown();
  }

  static Channel buildProxy(
      EventLoopGroup group,
      SslContext sslContext,
      ChannelHandlerContext ctx,
      InetSocketAddress address) {

    Bootstrap client =
        new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(
                new ChannelInitializer<Channel>() {
                  @Override
                  protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline()
                        .addLast(
                            sslContext.newHandler(
                                ch.alloc(), address.getHostString(), address.getPort()))
                        .addLast(
                            "codec",
                            new Http2HandlerBuilder(Http2FrameForwarder::create)
                                .server(false)
                                .build())
                        .addLast("proxy", new RawBackendHandler(ctx));
                  }
                })
            .remoteAddress(address);
    Channel ch = client.connect().syncUninterruptibly().channel();
    return ch;
  }

  @Test
  public void testGrpcProxyRequest() throws Exception {
    GrpcServer server = GrpcServer.run();
    // TODO(CK): this creates global state across tests we should do something smarter
    System.setProperty("xio.baseClient.remotePort", Integer.toString(server.getPort()));
    System.setProperty("xio.testProxyRoute.proxyPath", "/");
    ConfigFactory.invalidateCaches();
    Config root = ConfigFactory.load();

    ClientConfig config = ClientConfig.fromConfig("xio.h2TestClient");
    // ProxyConfig proxyConfig = ProxyConfig.parse("https://127.0.0.1:" + server.getPort() + "/");
    ProxyRouteConfig proxyConfig =
        new ProxyRouteConfig(ConfigFactory.load().getConfig("xio.testProxyRoute"));
    ApplicationConfig appConfig = ApplicationConfig.fromConfig("xio.testApplication", root);
    ClientFactory factory =
        new ClientFactory(new XioTracing(appConfig.getTracingConfig())) {
          @Override
          public Client createClient(ChannelHandlerContext ctx, ClientConfig config) {
            ClientState clientState = new ClientState(channelConfig(ctx), config);
            ClientChannelInitializer clientChannelInit =
                new ClientChannelInitializer(
                    clientState, () -> new ProxyBackendHandler(ctx), getTracing());
            ClientConnectionManager connManager =
                new ClientConnectionManager(clientState, clientChannelInit);
            return new Client(clientState, connManager);
          }
        };

    ApplicationState appState =
        new ApplicationState(ApplicationConfig.fromConfig("xio.defaultApplication"));
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.testGrpcServer");

    XioServerBootstrap bootstrap =
        new XioServerBootstrap(appState, serverConfig, new XioServerState(serverConfig))
            .addToPipeline(
                new SmartHttpPipeline() {
                  @Override
                  public ChannelHandler getApplicationRouter() {
                    return new PipelineRouter(
                        ImmutableMap.of(
                            "none",
                            ProxyRouteState.create(
                                appState,
                                proxyConfig,
                                new ProxyHandler(
                                    factory, proxyConfig, new SocketAddressHelper()))));
                  }
                });

    XioServer xioServer = bootstrap.build();
    GrpcClient client = GrpcClient.run(xioServer.getPort());

    HelloReply response = client.greet("world");

    assertEquals("Hello world", response.getMessage());

    client.shutdown();
    xioServer.close();
    server.stop();
  }
}
