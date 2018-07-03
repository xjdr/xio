package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import com.google.common.util.concurrent.Uninterruptibles;
import com.xjeffrose.xio.http.internal.Http2HeadersWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Http1ServerCodecUnitTest extends Assert {

  EmbeddedChannel channel = new EmbeddedChannel();
  List<Request> requests = new ArrayList<>();
  List<HttpObject> responses = new ArrayList<>();
  CountDownLatch outputReceived;

  @Before
  public void setUp() {
    channel
        .pipeline()
        .addLast(
            "nextResponse",
            new ChannelOutboundHandlerAdapter() {
              @Override
              public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                responses.add((HttpObject) msg);
                outputReceived.countDown();
              }
            });
    channel.pipeline().addLast("handler", new Http1ServerCodec());
    channel
        .pipeline()
        .addLast(
            "nextRequest",
            new SimpleChannelInboundHandler<Request>() {
              @Override
              protected void channelRead0(ChannelHandlerContext ctx, Request msg) {
                requests.add(msg);
                outputReceived.countDown();
              }
            });
  }

  @Test
  public void testFullRequest() throws Exception {
    outputReceived = new CountDownLatch(1);

    FullHttpRequest requestIn = new DefaultFullHttpRequest(HTTP_1_1, GET, "/");
    channel.writeInbound(requestIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Request requestOut = requests.remove(0);

    assertTrue(requestOut != null);
    assertTrue(requestOut instanceof FullRequest);
    assertEquals("HTTP/1.1", requestOut.version());
    assertEquals(HttpMethod.GET, requestOut.method());
    assertEquals("/", requestOut.path());
    assertFalse(requestOut.hasBody());
    assertFalse(requestOut.body() == null);
    assertEquals(0, requestOut.body().readableBytes());
  }

  @Test
  public void testFullRequestWithBody() throws Exception {
    outputReceived = new CountDownLatch(1);
    String payload = "body";
    ByteBuf body = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, payload);
    FullHttpRequest requestIn = new DefaultFullHttpRequest(HTTP_1_1, GET, "/", body);

    channel.writeInbound(requestIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Request requestOut = requests.remove(0);

    assertTrue(requestOut != null);
    assertTrue(requestOut instanceof FullRequest);
    assertEquals("HTTP/1.1", requestOut.version());
    assertEquals(HttpMethod.GET, requestOut.method());
    assertEquals("/", requestOut.path());
    assertTrue(requestOut.hasBody());
    assertFalse(requestOut.body() == null);
    assertEquals(body, requestOut.body());
  }

  @Test
  public void testStreamingRequest() throws Exception {
    outputReceived = new CountDownLatch(3);
    HttpRequest requestIn = new DefaultHttpRequest(HTTP_1_1, GET, "/");
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    HttpContent content = new DefaultHttpContent(body1);
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    LastHttpContent lastContent = new DefaultLastHttpContent(body2);

    channel.writeInbound(requestIn);
    channel.writeInbound(content);
    channel.writeInbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Request requestOut = requests.remove(0);

    assertTrue(requestOut != null);
    assertTrue(requestOut instanceof SegmentedRequest);
    assertEquals("HTTP/1.1", requestOut.version());
    assertEquals(HttpMethod.GET, requestOut.method());
    assertEquals("/", requestOut.path());
    assertFalse(requestOut.hasBody());
    assertFalse(requestOut.body() == null);
    assertEquals(0, requestOut.body().readableBytes());

    Request bodyOut1 = requests.remove(0);

    assertTrue(bodyOut1 != null);
    assertTrue(bodyOut1 instanceof SegmentedRequestData);
    assertEquals("HTTP/1.1", bodyOut1.version());
    assertEquals(HttpMethod.GET, bodyOut1.method());
    assertEquals("/", bodyOut1.path());
    assertFalse(bodyOut1.hasBody());
    assertFalse(bodyOut1.body() == null);
    assertFalse(((SegmentedRequestData) bodyOut1).content() == null);
    assertEquals(body1, ((SegmentedRequestData) bodyOut1).content());
    assertFalse(((SegmentedRequestData) bodyOut1).endOfMessage());

    Request bodyOut2 = requests.remove(0);

    assertTrue(bodyOut2 != null);
    assertTrue(bodyOut2 instanceof SegmentedRequestData);
    assertEquals("HTTP/1.1", bodyOut2.version());
    assertEquals(HttpMethod.GET, bodyOut2.method());
    assertEquals("/", bodyOut2.path());
    assertFalse(bodyOut2.hasBody());
    assertFalse(bodyOut2.body() == null);
    assertFalse(((SegmentedRequestData) bodyOut2).content() == null);
    assertEquals(body2, ((SegmentedRequestData) bodyOut2).content());
    assertTrue(((SegmentedRequestData) bodyOut2).endOfMessage());
  }

  @Test
  public void testFullResponse() throws Exception {
    outputReceived = new CountDownLatch(2);
    ByteBuf body = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "response");

    FullHttpRequest requestIn = new DefaultFullHttpRequest(HTTP_1_1, GET, "/");
    FullResponse responseIn = ResponseBuilders.newOk().body(body).build();

    channel.writeInbound(requestIn);
    channel.runPendingTasks(); // blocks
    channel.writeOutbound(responseIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    HttpResponse responseOut = (HttpResponse) responses.remove(0);

    assertTrue(responseOut != null);
    assertTrue(responseOut instanceof FullHttpResponse);
    assertEquals(HTTP_1_1, responseOut.protocolVersion());
    assertEquals(OK, responseOut.status());
    assertFalse(((FullHttpResponse) responseOut).content() == null);
    assertEquals(body, ((FullHttpResponse) responseOut).content());

    // https://tools.ietf.org/html/rfc7230#section-3.3.2
    assertNotNull(responseOut.headers().get(HttpHeaderNames.CONTENT_LENGTH));
    assertNull(responseOut.headers().get(HttpHeaderNames.TRANSFER_ENCODING));
  }

  @Test
  public void testStreamingResponse() throws Exception {
    outputReceived = new CountDownLatch(3);
    ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "response");

    FullHttpRequest requestIn = new DefaultFullHttpRequest(HTTP_1_1, GET, "/");

    SegmentedResponse responseIn =
        DefaultSegmentedResponse.builder().status(OK).headers(new DefaultHeaders()).build();
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    SegmentedData content =
        DefaultSegmentedData.builder().content(body1).endOfMessage(false).build();
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    SegmentedData lastContent =
        DefaultSegmentedData.builder()
            .content(body2)
            .endOfMessage(true)
            .trailingHeaders(new DefaultHeaders())
            .build();

    channel.writeInbound(requestIn);
    channel.runPendingTasks(); // blocks
    channel.writeOutbound(responseIn);
    channel.writeOutbound(content);
    channel.writeOutbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    HttpResponse responseOut = (HttpResponse) responses.remove(0);

    assertTrue(responseOut != null);
    assertTrue(responseOut instanceof HttpResponse);
    assertEquals(HTTP_1_1, responseOut.protocolVersion());
    assertEquals(OK, responseOut.status());

    HttpContent bodyOut1 = (HttpContent) responses.remove(0);

    assertTrue(bodyOut1 != null);
    assertFalse(bodyOut1.content() == null);
    assertEquals(body1, bodyOut1.content());

    LastHttpContent bodyOut2 = (LastHttpContent) responses.remove(0);

    assertTrue(bodyOut2 != null);
    assertFalse(bodyOut2.content() == null);
    assertEquals(body2, bodyOut2.content());

    // https://tools.ietf.org/html/rfc7230#section-3.3.2
    assertEquals(
        HttpHeaderValues.CHUNKED.toString(),
        responseOut.headers().get(HttpHeaderNames.TRANSFER_ENCODING));
    assertNull(responseOut.headers().get(HttpHeaderNames.CONTENT_LENGTH));
  }

  @Test
  public void testStreamingProxiedH2Response() throws Exception {
    outputReceived = new CountDownLatch(3);
    ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "response");

    FullHttpRequest requestIn = new DefaultFullHttpRequest(HTTP_1_1, GET, "/");

    Headers headers = new Http2HeadersWrapper(new DefaultHttp2Headers());
    SegmentedResponse responseIn =
        DefaultSegmentedResponse.builder().status(OK).headers(headers).build();
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    SegmentedData content =
        DefaultSegmentedData.builder().content(body1).endOfMessage(false).build();
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    SegmentedData lastContent =
        DefaultSegmentedData.builder()
            .content(body2)
            .endOfMessage(true)
            .trailingHeaders(new DefaultHeaders())
            .build();

    channel.writeInbound(requestIn);
    channel.runPendingTasks(); // blocks
    channel.writeOutbound(responseIn);
    channel.writeOutbound(content);
    channel.writeOutbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    HttpResponse responseOut = (HttpResponse) responses.remove(0);

    // https://tools.ietf.org/html/rfc7230#section-3.3.2
    assertEquals(
        HttpHeaderValues.CHUNKED.toString(),
        responseOut.headers().get(HttpHeaderNames.TRANSFER_ENCODING));
    assertNull(responseOut.headers().get(HttpHeaderNames.CONTENT_LENGTH));
  }
}
