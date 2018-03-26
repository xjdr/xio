package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import com.xjeffrose.xio.tracing.XioTracing;
import helloworld.*;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

@Slf4j
public class GrpcFunctionalTest extends Assert {

  public static class HelloWorldClient {

    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    private static ManagedChannel build(String host, int port) {
      return NettyChannelBuilder.forAddress(host, port)
          // this overrides dns lookup, maybe
          // .overrideAuthority(TestUtils.TEST_SERVER_HOST)
          .overrideAuthority(host + ":" + port)
          // this is the default
          // .negotiationType(NegotiationType.TLS)
          .sslContext(
              SslContextFactory.buildClientContext(
                  TlsConfig.fromConfig("xio.h2TestClient.settings.tls"),
                  InsecureTrustManagerFactory.INSTANCE))
          .build();
    }

    /** Construct client connecting to HelloWorld server at {@code host:port}. */
    public HelloWorldClient(String host, int port) {
      this(build(host, port));
    }

    /** Construct client for accessing RouteGuide server using the existing channel. */
    HelloWorldClient(ManagedChannel channel) {
      this.channel = channel;
      blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Say hello to server. */
    public HelloReply greet(String name) {
      HelloRequest request = HelloRequest.newBuilder().setName(name).build();
      HelloReply response;
      response = blockingStub.sayHello(request);
      return response;
    }

    public static HelloWorldClient run(int port) throws Exception {
      HelloWorldClient client = new HelloWorldClient("127.0.0.1", port);
      return client;
    }
  }

  public static class HelloWorldServer {

    private Server server;

    private void start(int port) throws IOException {
      server =
          NettyServerBuilder.forPort(port)
              .sslContext(
                  SslContextFactory.buildServerContext(
                      TlsConfig.fromConfig("xio.testServer.settings.tls")))
              .addService(new GreeterImpl())
              .build()
              .start();
    }

    public void stop() {
      if (server != null) {
        server.shutdown();
      }
    }

    private void blockUntilShutdown() throws InterruptedException {
      if (server != null) {
        server.awaitTermination();
      }
    }

    public int getPort() {
      return server.getPort();
    }

    public static HelloWorldServer run() throws IOException, InterruptedException {
      final HelloWorldServer server = new HelloWorldServer();
      server.start(0);
      return server;
    }

    static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

      @Override
      public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
      }
    }
  }

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
    HelloWorldServer server = HelloWorldServer.run();
    HelloWorldClient client = HelloWorldClient.run(server.getPort());

    HelloReply response = client.greet("world");

    assertEquals("Expected Response", "Hello world", response.getMessage());

    client.shutdown();
    server.stop();
    server.blockUntilShutdown();
  }

  @Test
  public void testFakeGrpcServer() throws Exception {
    final Http2Headers cannedHeaders = new DefaultHttp2Headers();
    cannedHeaders
        .status("200")
        .add("content-type", "application/grpc")
        .add("grpc-encoding", "identity")
        .add("grpc-accept-encoding", "gzip");

    final Http2Headers cannedTrailers = new DefaultHttp2Headers().add("grpc-status", "0");

    ByteBuf buf =
        Unpooled.copiedBuffer(ByteBufUtil.decodeHexDump("000000000d0a0b48656c6c6f20776f726c64"));
    final Http2DataFrame cannedData = new DefaultHttp2DataFrame(buf.retain(), false);

    XioServerBootstrap bootstrap =
        XioServerBootstrap.fromConfig("xio.testGrpcServer")
            .addToPipeline(
                new SmartHttpPipeline() {
                  @Override
                  public ChannelHandler getApplicationRouter() {
                    return new PipelineRouter(
                        ImmutableMap.of(),
                        new PipelineRequestHandler() {
                          @Override
                          public void handle(
                              ChannelHandlerContext ctx, Request request, RouteState route) {
                            if (request instanceof StreamingRequestData) {
                              StreamingRequestData streaming = (StreamingRequestData) request;

                              if (streaming.endOfStream()) {
                                ctx.write(Http2Response.build(request.streamId(), cannedHeaders));
                                ctx.write(
                                    Http2Response.build(request.streamId(), cannedData, false));
                                ctx.write(
                                    Http2Response.build(request.streamId(), cannedTrailers, true));
                              }
                            }
                          }
                        });
                  }
                });

    XioServer xioServer = bootstrap.build();
    HelloWorldClient client = HelloWorldClient.run(xioServer.getPort());

    HelloReply response = client.greet("world");

    assertEquals("Hello world", response.getMessage());

    client.shutdown();
    xioServer.close();
  }

  @Test
  public void testFakeGrpcClient() throws Exception {
    HelloWorldServer server = HelloWorldServer.run();

    InetSocketAddress boundAddress = new InetSocketAddress("127.0.0.1", server.getPort());

    final SslContext sslContext =
        SslContextFactory.buildClientContext(
            TlsConfig.fromConfig("xio.h2TestClient.settings.tls"),
            InsecureTrustManagerFactory.INSTANCE);

    CountDownLatch msgReceived = new CountDownLatch(2);
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
                                ch.alloc(), boundAddress.getHostString(), boundAddress.getPort()))
                        .addLast(
                            "codec",
                            new Http2HandlerBuilder(
                                    (s) ->
                                        new Http2FrameAdapter() {
                                          @Override
                                          public int onDataRead(
                                              ChannelHandlerContext ctx,
                                              int streamId,
                                              ByteBuf data,
                                              int padding,
                                              boolean endOfStream)
                                              throws Http2Exception {
                                            msgReceived.countDown();
                                            return data.readableBytes() + padding;
                                          }

                                          @Override
                                          public void onHeadersRead(
                                              ChannelHandlerContext ctx,
                                              int streamId,
                                              Http2Headers headers,
                                              int padding,
                                              boolean endStream)
                                              throws Http2Exception {
                                            msgReceived.countDown();
                                          }

                                          @Override
                                          public void onHeadersRead(
                                              ChannelHandlerContext ctx,
                                              int streamId,
                                              Http2Headers headers,
                                              int streamDependency,
                                              short weight,
                                              boolean exclusive,
                                              int padding,
                                              boolean endStream)
                                              throws Http2Exception {
                                            msgReceived.countDown();
                                          }
                                        })
                                .server(false)
                                .build());
                  }
                })
            .remoteAddress(boundAddress);

    Channel ch = client.connect().syncUninterruptibly().channel();

    Http2Headers headers = new DefaultHttp2Headers();
    headers
        .authority("127.0.0.1:61422")
        .method("POST")
        .path("/helloworld.Greeter/SayHello")
        .scheme("https")
        .add("content-type", "application/grpc")
        .add("te", "trailers")
        .add("user-agent", "grpc-java-netty/1.7.0")
        .add("grpc-accept-encoding", "gzip")
        .add("grpc-trace-bin", "");

    ByteBuf buf = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("00000000070a05776f726c64"));
    Http2DataFrame data = new DefaultHttp2DataFrame(buf, true);
    ch.write(headers);
    ch.writeAndFlush(data).awaitUninterruptibly();
    Uninterruptibles.awaitUninterruptibly(msgReceived);
    server.stop();
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
                        .addLast("stream mapper", new Http2StreamMapper())
                        .addLast("proxy", new RawBackendHandler(ctx));
                  }
                })
            .remoteAddress(address);
    Channel ch = client.connect().syncUninterruptibly().channel();
    return ch;
  }

  @Test
  public void testGrpcProxyRequest() throws Exception {
    HelloWorldServer server = HelloWorldServer.run();
    // TODO(CK): this creates global state across tests we should do something smarter
    System.setProperty("xio.baseClient.remotePort", Integer.toString(server.getPort()));
    System.setProperty("xio.testProxyRoute.proxyPath", "/");
    ConfigFactory.invalidateCaches();
    Config root = ConfigFactory.load();

    ClientConfig config = ClientConfig.fromConfig("xio.h2TestClient");
    // ProxyConfig proxyConfig = ProxyConfig.parse("https://127.0.0.1:" + server.getPort() + "/");
    ProxyRouteConfig proxyConfig =
        new ProxyRouteConfig(ConfigFactory.load().getConfig("xio.testProxyRoute"));
    ClientFactory factory =
        new ClientFactory() {
          @Override
          public Client createClient(
              ChannelHandlerContext ctx, ClientConfig config, XioTracing tracing) {
            ClientState clientState = new ClientState(channelConfig(ctx), config);
            return new Client(clientState, () -> new ProxyBackendHandler(ctx), tracing);
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
                            "*",
                            new ProxyRouteState(
                                appState,
                                proxyConfig,
                                new ProxyHandler(
                                    factory,
                                    proxyConfig,
                                    new SocketAddressHelper(),
                                    appState.tracing()))));
                  }
                });

    XioServer xioServer = bootstrap.build();
    HelloWorldClient client = HelloWorldClient.run(xioServer.getPort());

    HelloReply response = client.greet("world");

    assertEquals("Hello world", response.getMessage());

    client.shutdown();
    xioServer.close();
    server.stop();
  }
}
