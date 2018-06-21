package com.xjeffrose.xio.http;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.http.test_helpers.GrpcClient;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import helloworld.HelloReply;
import helloworld.HelloRequest;
import io.netty.channel.ChannelHandler;
import org.junit.Test;

public class GrpcRoutingTest {
  @Test
  public void testGrpcRoutedRequest() throws Exception {
    ApplicationState appState =
        new ApplicationState(ApplicationConfig.fromConfig("xio.defaultApplication"));
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.testGrpcServer");
    String sayHelloPath = "/helloworld.Greeter/SayHello";

    XioServerBootstrap bootstrap =
        new XioServerBootstrap(appState, serverConfig, new XioServerState(serverConfig))
            .addToPipeline(
                new SmartHttpPipeline() {
                  @Override
                  public ChannelHandler getApplicationRouter() {
                    RouteConfig config =
                        new RouteConfig(ConfigFactory.load().getConfig("xio.defaultRoute"));
                    GrpcRequestHandler handler =
                        new GrpcRequestHandler<>(
                            HelloRequest::parseFrom,
                            (HelloRequest request) ->
                                HelloReply.newBuilder()
                                    .setMessage("Hello " + request.getName())
                                    .build());
                    return new PipelineRouter(
                        ImmutableMap.of(
                            sayHelloPath,
                            new RouteState(
                                (ignored) -> Route.build(sayHelloPath), config, handler)));
                  }
                });

    XioServer xioServer = bootstrap.build();
    GrpcClient client = GrpcClient.run(xioServer.getPort());

    HelloReply response = client.greet("world");

    assertEquals("Hello world", response.getMessage());

    client.shutdown();
    xioServer.close();
  }
}
