package com.xjeffrose.xio.client;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.client.Http;
import com.xjeffrose.xio.mux.Request;
import com.xjeffrose.xio.core.FrameLengthCodec;
import com.xjeffrose.xio.mux.Codec;
import com.xjeffrose.xio.mux.LocalConnector;
import com.xjeffrose.xio.mux.ConnectionPool;
import com.xjeffrose.xio.mux.Response;
import com.xjeffrose.xio.mux.ServerCodec;
import com.xjeffrose.xio.mux.ServerRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RequestMuxerFunctionalTest extends Assert {

  RequestMuxer client;

  LocalServerChannel server;

  EventLoopGroup testGroup;

  LocalConnector connector;

  ConnectionPool connectionPool;

  HttpResponse responsePayload;

  BlockingQueue<ServerRequest> requests = Queues.newLinkedBlockingQueue();

  private ChannelHandler requestHandler() {
    return new SimpleChannelInboundHandler<ServerRequest>() {
      @Override
      protected void channelRead0(ChannelHandlerContext ctx, ServerRequest request) throws Exception {
        requests.offer(request);
        if (request.expectsResponse()) {
          Response response = new Response(request.getId(), responsePayload);
          ctx.writeAndFlush(response, ctx.newPromise());
        }
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("EXCEPTION: " + cause);
        cause.printStackTrace();
      }
    };
  }

  @Before
  public void setUp() throws Exception {
    String responseContent = "this is the response content";
    HttpHeaders headers = new DefaultHttpHeaders();
    headers.set(HttpHeaderNames.CONTENT_LENGTH, responseContent.length());
    responsePayload = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.OK,
      Unpooled.wrappedBuffer(responseContent.getBytes()),
      headers,
      new DefaultHttpHeaders()
    );

    testGroup = new DefaultEventLoopGroup();

    Config config = ConfigFactory.load().getConfig("xio.testApplication.settings.requestMuxer");

    LocalAddress address = new LocalAddress("functional-test-mux");

    ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
      @Override
      public void initChannel(Channel channel) {
        //channel.pipeline().addLast(new LoggingHandler(LogLevel.ERROR));
      }
    };

    ChannelInitializer<Channel> childInitializer = new ChannelInitializer<Channel>() {
      @Override
      public void initChannel(Channel channel) {
        channel.pipeline()
          //.addLast(new LoggingHandler(LogLevel.ERROR))
          .addLast("frame length codec", new FrameLengthCodec())
          .addLast("mux message codec", new Codec())
          .addLast("http request decoder", new HttpRequestDecoder())
          .addLast("http reponse encoder", new HttpResponseEncoder())
          .addLast("http request aggregator", new HttpObjectAggregator(65535))
          .addLast("mux server codec", new ServerCodec())
          .addLast("mux server request handler", requestHandler())
          ;
      }
    };

    server = (LocalServerChannel) new ServerBootstrap()
      .localAddress(address)
      .channel(LocalServerChannel.class)
      .group(testGroup)
      .handler(initializer)
      .childHandler(childInitializer)
      .bind()
      .syncUninterruptibly() //block
      .channel()
      ;

    connector = new LocalConnector(address) {
      @Override
      protected List<Map.Entry<String, ChannelHandler>> payloadHandlers() {
        return Arrays.asList(
          new AbstractMap.SimpleImmutableEntry<>("http request encoder", new HttpRequestEncoder()),
          new AbstractMap.SimpleImmutableEntry<>("http response decoder", new HttpResponseDecoder()),
          new AbstractMap.SimpleImmutableEntry<>("http request aggregator", new HttpObjectAggregator(65535))
        );
      }

      @Override
      protected EventLoopGroup group() {
        return testGroup;
      }
    };

    connectionPool = new ConnectionPool(connector);

    client = new RequestMuxer(
      config,
      testGroup,
      connectionPool
    );
    client.start();

  }

  @After
  public void tearDown() {
    client.close();
    server.close();
  }

  //@Test
  public void testRequestNoResponse() throws ExecutionException {
    HttpRequest httpRequest = Http.get("hostname.com", "/path");
    Request request = client.write(httpRequest);
    UUID id = Uninterruptibles.getUninterruptibly(request.getWriteFuture());

    ServerRequest serverRequest = Uninterruptibles.takeUninterruptibly(requests);
    HttpRequest serverHttpRequest = (HttpRequest)serverRequest.getPayload();
    assertEquals(id, serverRequest.getId());
    assertFalse(serverRequest.expectsResponse());
    assertEquals(httpRequest.method(), serverHttpRequest.method());
    assertEquals(httpRequest.uri(), serverHttpRequest.uri());
    assertEquals(httpRequest.protocolVersion(), serverHttpRequest.protocolVersion());
    assertEquals(httpRequest.headers().size(), serverHttpRequest.headers().size());
    for (Map.Entry<String, String> header : httpRequest.headers()) {
      assertTrue(serverHttpRequest.headers().contains(header.getKey(), header.getValue(), false));
    }
  }

  @Test
  public void testRequestExpectsResponse() throws ExecutionException {
    HttpRequest httpRequest = Http.post("hostname.com", "/path", "this is the payload");
    Request request = client.writeExpectResponse(httpRequest);
    UUID id = Uninterruptibles.getUninterruptibly(request.getWriteFuture());

    ServerRequest serverRequest = Uninterruptibles.takeUninterruptibly(requests);
    HttpRequest serverHttpRequest = (HttpRequest)serverRequest.getPayload();
    assertEquals(id, serverRequest.getId());
    assertTrue(serverRequest.expectsResponse());
    assertEquals(httpRequest.method(), serverHttpRequest.method());
    assertEquals(httpRequest.uri(), serverHttpRequest.uri());
    assertEquals(httpRequest.protocolVersion(), serverHttpRequest.protocolVersion());
    assertEquals(httpRequest.headers().size(), serverHttpRequest.headers().size());
    for (Map.Entry<String, String> header : httpRequest.headers()) {
      assertTrue(serverHttpRequest.headers().contains(header.getKey(), header.getValue(), false));
    }

    Response response = Uninterruptibles.getUninterruptibly(request.getResponseFuture());
    assertEquals(request.getId(), response.getInResponseTo());
    HttpResponse responsePayload = (HttpResponse)response.getPayload();
  }

}
