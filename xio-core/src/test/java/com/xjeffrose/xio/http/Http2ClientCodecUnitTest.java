package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class Http2ClientCodecUnitTest extends Assert {

  EmbeddedChannel channel = new EmbeddedChannel();
  List<Http2Request> requests = new ArrayList<>();
  List<Response> responses = new ArrayList<>();
  CountDownLatch outputReceived;

  @Before
  public void setUp() {
    channel
        .pipeline()
        .addLast(
            "nextRequest",
            new ChannelOutboundHandlerAdapter() {
              @Override
              public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                log.debug("request: " + msg);
                requests.add((Http2Request) msg);
                outputReceived.countDown();
              }
            });
    channel.pipeline().addLast("handler", new Http2ClientCodec());
    channel
        .pipeline()
        .addLast(
            "nextResponse",
            new SimpleChannelInboundHandler<Response>() {
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

    FullRequest requestIn = RequestBuilders.newGet("/").host("localhost").build();

    channel.writeOutbound(requestIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Http2Request requestOut = (Http2Request) requests.remove(0);

    assertTrue(requestOut != null);
    assertTrue(requestOut.payload instanceof Http2Headers);
    assertEquals("GET", ((Http2Headers) requestOut.payload).method().toString());
    assertEquals("/", ((Http2Headers) requestOut.payload).path());
    assertTrue(requestOut.eos);
  }

  @Test
  public void testFullRequestWithBody() throws Exception {
    outputReceived = new CountDownLatch(1);
    ByteBuf body = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body");
    FullRequest requestIn = RequestBuilders.newPost("/").host("localhost").body(body).build();

    channel.writeOutbound(requestIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Http2Request requestOut = (Http2Request) requests.remove(0);

    assertTrue(requestOut != null);
    assertTrue(requestOut.payload instanceof Http2Headers);
    assertEquals("POST", ((Http2Headers) requestOut.payload).method().toString());
    assertEquals("/", ((Http2Headers) requestOut.payload).path());
    assertFalse(requestOut.eos);

    Http2Request contentOut = (Http2Request) requests.remove(0);

    assertTrue(contentOut != null);
    assertTrue(contentOut.payload instanceof Http2DataFrame);
    assertEquals(body, ((Http2DataFrame) contentOut.payload).content());
    assertTrue(contentOut.eos);
  }

  @Test
  public void testStreamingRequest() throws Exception {
    outputReceived = new CountDownLatch(3);

    SegmentedRequest requestIn =
        DefaultSegmentedRequest.builder().method(POST).host("localhost").path("/").build();
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

    channel.writeOutbound(requestIn);
    channel.writeOutbound(content);
    channel.writeOutbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Http2Request requestOut = (Http2Request) requests.remove(0);

    assertTrue(requestOut != null);
    assertTrue(requestOut.payload instanceof Http2Headers);
    assertEquals("POST", ((Http2Headers) requestOut.payload).method().toString());
    assertEquals("/", ((Http2Headers) requestOut.payload).path());
    assertFalse(requestOut.eos);

    Http2Request bodyOut1 = (Http2Request) requests.remove(0);

    assertTrue(bodyOut1 != null);
    assertTrue(bodyOut1.payload instanceof Http2DataFrame);
    assertFalse(((Http2DataFrame) bodyOut1.payload).content() == null);
    assertEquals(body1, ((Http2DataFrame) bodyOut1.payload).content());
    assertFalse(bodyOut1.eos);

    Http2Request bodyOut2 = (Http2Request) requests.remove(0);

    assertTrue(bodyOut2 != null);
    assertTrue(bodyOut2.payload instanceof Http2DataFrame);
    assertFalse(((Http2DataFrame) bodyOut2.payload).content() == null);
    assertEquals(body2, ((Http2DataFrame) bodyOut2.payload).content());
    assertTrue(bodyOut2.eos);
  }

  @Test
  public void testStreamingRequestWithTrailingHeaders() throws Exception {
    outputReceived = new CountDownLatch(4);

    SegmentedRequest requestIn =
        DefaultSegmentedRequest.builder().method(POST).host("localhost").path("/").build();
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    SegmentedData content =
        DefaultSegmentedData.builder().content(body1).endOfMessage(false).build();
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    SegmentedData lastContent =
        DefaultSegmentedData.builder()
            .content(body2)
            .endOfMessage(true)
            .trailingHeaders(new DefaultHeaders().set("foo", "bar"))
            .build();

    channel.writeOutbound(requestIn);
    channel.writeOutbound(content);
    channel.writeOutbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Http2Request requestOut = (Http2Request) requests.remove(0);

    assertTrue(requestOut != null);
    assertTrue(requestOut.payload instanceof Http2Headers);
    assertEquals("POST", ((Http2Headers) requestOut.payload).method().toString());
    assertEquals("/", ((Http2Headers) requestOut.payload).path());
    assertFalse(requestOut.eos);

    Http2Request bodyOut1 = (Http2Request) requests.remove(0);

    assertTrue(bodyOut1 != null);
    assertTrue(bodyOut1.payload instanceof Http2DataFrame);
    assertFalse(((Http2DataFrame) bodyOut1.payload).content() == null);
    assertEquals(body1, ((Http2DataFrame) bodyOut1.payload).content());
    assertFalse(bodyOut1.eos);

    Http2Request bodyOut2 = (Http2Request) requests.remove(0);

    assertTrue(bodyOut2 != null);
    assertTrue(bodyOut2.payload instanceof Http2DataFrame);
    assertFalse(((Http2DataFrame) bodyOut2.payload).content() == null);
    assertEquals(body2, ((Http2DataFrame) bodyOut2.payload).content());
    assertFalse(bodyOut2.eos);

    Http2Request trailersOut = requests.remove(0);

    assertTrue(trailersOut != null);
    assertTrue(trailersOut.payload instanceof Http2Headers);
    assertEquals("bar", ((Http2Headers) trailersOut.payload).get("foo"));
    assertTrue(trailersOut.eos);
  }

  @Test
  public void testFullResponse() throws Exception {
    outputReceived = new CountDownLatch(1);
    Http2Headers headers = new DefaultHttp2Headers().status("200");
    Http2Response responseIn = Http2Response.build(1, headers, true);

    channel.writeInbound(responseIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Response responseOut = responses.remove(0);

    assertTrue(responseOut != null);
    assertTrue(responseOut instanceof FullResponse);
    assertEquals("h2", responseOut.version());
    assertEquals(OK, responseOut.status());
    assertFalse(responseOut.hasBody());
    assertEquals(1, responseOut.streamId());
  }

  @Test
  public void testStreamingResponse() throws Exception {
    outputReceived = new CountDownLatch(3);
    Http2Headers headers = new DefaultHttp2Headers().status("200");
    Http2Response responseIn = Http2Response.build(1, headers, false);
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    Http2Response content = Http2Response.build(1, new DefaultHttp2DataFrame(body1, false), false);
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    Http2Response lastContent =
        Http2Response.build(1, new DefaultHttp2DataFrame(body2, true), true);

    channel.writeInbound(responseIn);
    channel.writeInbound(content);
    channel.writeInbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    SegmentedResponse responseOut = (SegmentedResponse) responses.remove(0);

    assertTrue(responseOut != null);
    assertEquals("h2", responseOut.version());
    assertEquals(HttpResponseStatus.OK, responseOut.status());
    assertFalse(responseOut.hasBody());
    assertFalse(responseOut.body() == null);
    assertEquals(0, responseOut.body().readableBytes());

    SegmentedResponseData bodyOut1 = (SegmentedResponseData) responses.remove(0);

    assertTrue(bodyOut1 != null);
    assertEquals("h2", responseOut.version());
    assertEquals(HttpResponseStatus.OK, responseOut.status());
    assertFalse(bodyOut1.hasBody());
    assertFalse(bodyOut1.body() == null);
    assertFalse(bodyOut1.content() == null);
    assertEquals(body1, bodyOut1.content());
    assertFalse(bodyOut1.endOfMessage());

    SegmentedResponseData bodyOut2 = (SegmentedResponseData) responses.remove(0);

    assertTrue(bodyOut2 != null);
    assertEquals("h2", responseOut.version());
    assertEquals(HttpResponseStatus.OK, responseOut.status());
    assertFalse(bodyOut2.hasBody());
    assertFalse(bodyOut2.body() == null);
    assertFalse(bodyOut2.content() == null);
    assertEquals(body2, bodyOut2.content());
    assertTrue(bodyOut2.endOfMessage());
  }

  @Test
  public void testStreamingResponseWithTrailingHeaders() throws Exception {
    outputReceived = new CountDownLatch(3);
    Http2Headers headers = new DefaultHttp2Headers().status("200");
    Http2Response responseIn = Http2Response.build(1, headers, false);
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    Http2Response content = Http2Response.build(1, new DefaultHttp2DataFrame(body1, false), false);
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    Http2Response lastContent =
        Http2Response.build(1, new DefaultHttp2DataFrame(body2, false), false);
    Http2Response trailers =
        Http2Response.build(1, new DefaultHttp2Headers().set("foo", "bar"), true);

    channel.writeInbound(responseIn);
    channel.writeInbound(content);
    channel.writeInbound(lastContent);
    channel.writeInbound(trailers);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    SegmentedResponse responseOut = (SegmentedResponse) responses.remove(0);

    assertTrue(responseOut != null);
    assertEquals("h2", responseOut.version());
    assertEquals(HttpResponseStatus.OK, responseOut.status());
    assertFalse(responseOut.hasBody());
    assertFalse(responseOut.body() == null);
    assertEquals(0, responseOut.body().readableBytes());

    SegmentedResponseData bodyOut1 = (SegmentedResponseData) responses.remove(0);

    assertTrue(bodyOut1 != null);
    assertEquals("h2", responseOut.version());
    assertEquals(HttpResponseStatus.OK, responseOut.status());
    assertFalse(bodyOut1.hasBody());
    assertFalse(bodyOut1.body() == null);
    assertFalse(bodyOut1.content() == null);
    assertEquals(body1, bodyOut1.content());
    assertFalse(bodyOut1.endOfMessage());

    SegmentedResponseData bodyOut2 = (SegmentedResponseData) responses.remove(0);

    assertTrue(bodyOut2 != null);
    assertEquals("h2", responseOut.version());
    assertEquals(HttpResponseStatus.OK, responseOut.status());
    assertFalse(bodyOut2.hasBody());
    assertFalse(bodyOut2.body() == null);
    assertFalse(bodyOut2.content() == null);
    assertEquals(body2, bodyOut2.content());
    assertFalse(bodyOut2.endOfMessage());

    SegmentedResponseData trailersOut = (SegmentedResponseData) responses.remove(0);

    assertTrue(trailersOut != null);
    assertEquals("h2", trailersOut.version());
    assertEquals(HttpResponseStatus.OK, trailersOut.status());
    assertFalse(trailersOut.hasBody());
    assertFalse(trailersOut.body() == null);
    assertEquals(0, trailersOut.body().readableBytes());
    assertEquals(1, trailersOut.trailingHeaders().size());
    assertEquals("bar", trailersOut.trailingHeaders().get("foo"));
    assertTrue(trailersOut.endOfMessage());
  }
}
