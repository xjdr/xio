package com.xjeffrose.xio.http;

import com.google.common.util.concurrent.Uninterruptibles;
import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.pipeline.XioChannelHandlerFactory;
import com.xjeffrose.xio.server.XioServer;
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
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
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
import io.netty.util.AttributeKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

  @Before
  public void setUp() {
    group = new NioEventLoopGroup(2);
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

    XioChannelHandlerFactory f =
        () ->
            new ChannelInboundHandlerAdapter() {
              @Override
              public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof Http2Request) {
                  Http2Request request = (Http2Request) msg;
                  if (request.payload instanceof Http2DataFrame) {
                    ctx.write(Http2Response.build(request.streamId, cannedHeaders));
                    ctx.write(Http2Response.build(request.streamId, cannedData));
                    ctx.write(Http2Response.build(request.streamId, cannedTrailers, true));
                  }
                }
              }
            };
    XioServerBootstrap bootstrap =
        XioServerBootstrap.fromConfig("xio.testGrpcServer").addToPipeline(new SmartHttpPipeline(f));

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

  private static final AttributeKey<Channel> TEST_CH_KEY =
      AttributeKey.newInstance("xio_test_ch_key");

  @Test
  public void testGrpcProxyRequest() throws Exception {
    HelloWorldServer server = HelloWorldServer.run();

    final SslContext sslContext =
        SslContextFactory.buildClientContext(
            TlsConfig.fromConfig("xio.h2TestClient.settings.tls"),
            InsecureTrustManagerFactory.INSTANCE);

    InetSocketAddress boundAddress = new InetSocketAddress("127.0.0.1", server.getPort());

    XioChannelHandlerFactory f =
        () ->
            new ChannelDuplexHandler() {
              @Override
              public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                Channel ch = ctx.channel().attr(TEST_CH_KEY).get();
                if (ch == null) {
                  ch = buildProxy(group, sslContext, ctx, boundAddress);
                  ctx.channel().attr(TEST_CH_KEY).set(ch);
                }

                if (msg instanceof Http2Request) {
                  Http2Request request = (Http2Request) msg;
                  ch.writeAndFlush(request);
                }
              }

              @Override
              public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                  throws Exception {
                ctx.write(msg, promise);
              }
            };
    XioServerBootstrap bootstrap =
        XioServerBootstrap.fromConfig("xio.testGrpcServer").addToPipeline(new SmartHttpPipeline(f));

    XioServer xioServer = bootstrap.build();
    HelloWorldClient client = HelloWorldClient.run(xioServer.getPort());

    HelloReply response = client.greet("world");

    assertEquals("Hello world", response.getMessage());

    client.shutdown();
    xioServer.close();
    server.stop();
  }
}
