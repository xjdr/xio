package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static java.util.concurrent.TimeUnit.SECONDS;

import io.netty.handler.codec.http.HttpMethod;
import com.xjeffrose.xio.SSL.SelfSignedX509CertGenerator;
import com.xjeffrose.xio.SSL.X509Certificate;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.util.ArrayList;
import java.util.List;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

@Slf4j
public class Http1ClientCodecUnitTest extends Assert {

  EmbeddedChannel channel = new EmbeddedChannel();
  List<HttpObject> requests = new ArrayList<>();
  List<Response> responses = new ArrayList<>();
  CountDownLatch outputReceived;

  @Before
  public void setUp() {
    channel.pipeline().addLast("nextRequest", new ChannelOutboundHandlerAdapter() {
      @Override
      public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        log.debug("request: " + msg);
        requests.add((HttpObject)msg);
        outputReceived.countDown();
      }
    });
    channel.pipeline().addLast("handler", new Http1ClientCodec());
    channel.pipeline().addLast("nextResponse", new SimpleChannelInboundHandler<Response>() {
      @Override
      protected void channelRead0(ChannelHandlerContext ctx, Response msg) {
        log.debug("response: " + msg);
        responses.add(msg);
        outputReceived.countDown();
      }
    });
  }

  @Test
  public void testFullRequest() throws Exception {
    outputReceived = new CountDownLatch(1);

    FullRequest requestIn = RequestBuilders.newGet("/").build();

    channel.writeOutbound(requestIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    FullHttpRequest requestOut = (FullHttpRequest)requests.remove(0);

    assertTrue(requestOut != null);
    assertEquals(HTTP_1_1, requestOut.protocolVersion());
    assertEquals(HttpMethod.GET, requestOut.method());
    assertEquals("/", requestOut.uri());
    assertFalse(requestOut.content() == null);
    assertEquals(0, requestOut.content().readableBytes());
  }

  @Test
  public void testFullRequestWithBody() throws Exception {
    outputReceived = new CountDownLatch(1);
    ByteBuf body = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body");
    FullRequest requestIn = RequestBuilders.newPost("/").body(body).build();

    channel.writeOutbound(requestIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    FullHttpRequest requestOut = (FullHttpRequest)requests.remove(0);

    assertTrue(requestOut != null);
    assertEquals(HTTP_1_1, requestOut.protocolVersion());
    assertEquals(HttpMethod.POST, requestOut.method());
    assertEquals("/", requestOut.uri());
    assertFalse(requestOut.content() == null);
    assertEquals(body, requestOut.content());
  }

  @Test
  public void testStreamingRequest() throws Exception {
    outputReceived = new CountDownLatch(3);

    StreamingRequest requestIn = DefaultStreamingRequest.builder()
      .method(POST)
      .path("/")
      .headers(new DefaultHeaders())
      .build();
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    StreamingData content = DefaultStreamingData.builder()
      .content(body1)
      .endOfStream(false)
      .build();
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    StreamingData lastContent = DefaultStreamingData.builder()
      .content(body2)
      .endOfStream(true)
      .trailingHeaders(new DefaultHeaders())
      .build();

    channel.writeOutbound(requestIn);
    channel.writeOutbound(content);
    channel.writeOutbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    HttpRequest requestOut = (HttpRequest)requests.remove(0);

    assertTrue(requestOut != null);
    assertEquals(HTTP_1_1, requestOut.protocolVersion());
    assertEquals(HttpMethod.POST, requestOut.method());
    assertEquals("/", requestOut.uri());

    HttpContent bodyOut1 = (HttpContent)requests.remove(0);

    assertTrue(bodyOut1 != null);
    assertFalse(bodyOut1.content() == null);
    assertEquals(body1, bodyOut1.content());

    HttpContent bodyOut2 = (HttpContent)requests.remove(0);

    assertTrue(bodyOut2 != null);
    assertFalse(bodyOut2.content() == null);
    assertEquals(body2, bodyOut2.content());
  }

  @Test
  public void testFullResponse() throws Exception {
    outputReceived = new CountDownLatch(1);
    ByteBuf body = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "response");

    FullHttpResponse responseIn = new DefaultFullHttpResponse(HTTP_1_1,
                                                              OK,
                                                              body);

    channel.writeInbound(responseIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Response responseOut = responses.remove(0);

    assertTrue(responseOut != null);
    assertTrue(responseOut instanceof FullResponse);
    assertEquals("HTTP/1.1", responseOut.version());
    assertEquals(OK, responseOut.status());
    assertTrue(responseOut.hasBody());
    assertFalse(responseOut.body() == null);
    assertEquals(body, responseOut.body());
  }

  @Test
  public void testStreamingResponse() throws Exception {
    outputReceived = new CountDownLatch(3);
    HttpResponse responseIn = new DefaultHttpResponse(HTTP_1_1,
                                                      OK);
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    HttpContent content = new DefaultHttpContent(body1);
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    LastHttpContent lastContent = new DefaultLastHttpContent(body2);

    channel.writeInbound(responseIn);
    channel.writeInbound(content);
    channel.writeInbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    StreamingResponse responseOut = (StreamingResponse)responses.remove(0);

    assertTrue(responseOut != null);
    assertEquals("HTTP/1.1", responseOut.version());
    assertEquals(HttpResponseStatus.OK, responseOut.status());
    assertFalse(responseOut.hasBody());
    assertFalse(responseOut.body() == null);
    assertEquals(0, responseOut.body().readableBytes());

    StreamingResponseData bodyOut1 = (StreamingResponseData)responses.remove(0);

    assertTrue(bodyOut1 != null);
    assertEquals("HTTP/1.1", responseOut.version());
    assertEquals(HttpResponseStatus.OK, responseOut.status());
    assertFalse(bodyOut1.hasBody());
    assertFalse(bodyOut1.body() == null);
    assertFalse(bodyOut1.content() == null);
    assertEquals(body1, bodyOut1.content());
    assertFalse(bodyOut1.endOfStream());

    StreamingResponseData bodyOut2 = (StreamingResponseData)responses.remove(0);

    assertTrue(bodyOut2 != null);
    assertEquals("HTTP/1.1", responseOut.version());
    assertEquals(HttpResponseStatus.OK, responseOut.status());
    assertFalse(bodyOut2.hasBody());
    assertFalse(bodyOut2.body() == null);
    assertFalse(bodyOut2.content() == null);
    assertEquals(body2, bodyOut2.content());
    assertTrue(bodyOut2.endOfStream());
  }
}
