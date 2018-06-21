package com.xjeffrose.xio.http.test_helpers;

import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import helloworld.GreeterGrpc;
import helloworld.HelloReply;
import helloworld.HelloRequest;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;

public class GrpcServer {

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

  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public int getPort() {
    return server.getPort();
  }

  public static GrpcServer run() throws IOException, InterruptedException {
    final GrpcServer server = new GrpcServer();
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
