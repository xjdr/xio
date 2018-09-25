package com.xjeffrose.xio.http.test_helpers;

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.tls.SslContextFactory;
import com.xjeffrose.xio.tls.TlsConfig;
import helloworld.GreeterGrpc;
import helloworld.HelloReply;
import helloworld.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.concurrent.TimeUnit;

public class GrpcClient {

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
                TlsConfig.builderFrom(
                        ConfigFactory.load().getConfig("xio.h2TestClient.settings.tls"))
                    .build(),
                InsecureTrustManagerFactory.INSTANCE))
        .build();
  }

  /** Construct client connecting to HelloWorld server at {@code host:port}. */
  public GrpcClient(String host, int port) {
    this(build(host, port));
  }

  /** Construct client for accessing RouteGuide server using the existing channel. */
  GrpcClient(ManagedChannel channel) {
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

  public static GrpcClient run(int port) throws Exception {
    GrpcClient client = new GrpcClient("127.0.0.1", port);
    return client;
  }
}
