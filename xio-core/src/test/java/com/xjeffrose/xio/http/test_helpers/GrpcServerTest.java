package com.xjeffrose.xio.http.test_helpers;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.bootstrap.XioServiceLocator;
import com.xjeffrose.xio.http.Http2Response;
import com.xjeffrose.xio.http.PipelineRequestHandler;
import com.xjeffrose.xio.http.PipelineRouter;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.RouteState;
import com.xjeffrose.xio.http.SegmentedRequestData;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.server.XioServer;
import helloworld.HelloReply;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import org.junit.Test;

public class GrpcServerTest {

  @Test
  public void testGrpcServer() throws Exception {
    ApplicationConfig applicationConfig =
        ApplicationConfig.fromConfig("xio.defaultApplication", ConfigFactory.load());
    ApplicationState applicationState = new ApplicationState(applicationConfig);
    XioServiceLocator.TEST_ONLY_buildInstance(applicationConfig, applicationState);

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
                            if (request instanceof SegmentedRequestData) {
                              SegmentedRequestData streaming = (SegmentedRequestData) request;

                              if (streaming.endOfMessage()) {
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
    GrpcClient client = GrpcClient.run(xioServer.getPort());

    HelloReply response = client.greet("world");

    assertEquals("Hello world", response.getMessage());

    client.shutdown();
    xioServer.close();
  }
}
