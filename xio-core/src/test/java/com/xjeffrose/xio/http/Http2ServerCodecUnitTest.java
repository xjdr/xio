package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.CharsetUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Http2ServerCodecUnitTest extends Assert {

  EmbeddedChannel channel = new EmbeddedChannel();
  List<Request> requests = new ArrayList<>();
  List<Http2Response> responses = new ArrayList<>();
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
                responses.add((Http2Response) msg);
                outputReceived.countDown();
              }
            });
    channel.pipeline().addLast("handler", new Http2ServerCodec());
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

    Http2Headers headers = new DefaultHttp2Headers().method("GET").path("/");
    Http2Request requestIn = Http2Request.build(1, headers, true);

    channel.writeInbound(requestIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Request requestOut = requests.remove(0);

    assertNotNull(requestOut);
    assertTrue(requestOut instanceof FullRequest);
    assertEquals("h2", requestOut.version());
    assertEquals(HttpMethod.GET, requestOut.method());
    assertEquals("/", requestOut.path());
    assertFalse(requestOut.hasBody());
    assertNotNull(requestOut.body());
    assertEquals(0, requestOut.body().readableBytes());
    assertEquals(1, requestOut.streamId());
  }

  @Test
  public void testStreamingRequest() throws Exception {
    outputReceived = new CountDownLatch(3);

    Http2Headers headers = new DefaultHttp2Headers().method("POST").path("/");
    Http2Request requestIn = Http2Request.build(1, headers, false);
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    Http2Request content = Http2Request.build(1, new DefaultHttp2DataFrame(body1, false), false);
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    Http2Request lastContent = Http2Request.build(1, new DefaultHttp2DataFrame(body2, true), true);

    channel.writeInbound(requestIn);
    channel.writeInbound(content);
    channel.writeInbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Request requestOut = requests.remove(0);

    assertNotNull(requestOut);
    assertTrue(requestOut instanceof SegmentedRequest);
    assertEquals("h2", requestOut.version());
    assertEquals(HttpMethod.POST, requestOut.method());
    assertEquals("/", requestOut.path());
    assertFalse(requestOut.hasBody());
    assertNotNull(requestOut.body());
    assertEquals(0, requestOut.body().readableBytes());

    Request bodyOut1 = requests.remove(0);

    assertNotNull(bodyOut1);
    assertTrue(bodyOut1 instanceof SegmentedRequestData);
    assertEquals("h2", bodyOut1.version());
    assertEquals(HttpMethod.POST, bodyOut1.method());
    assertEquals("/", bodyOut1.path());
    assertFalse(bodyOut1.hasBody());
    assertNotNull(bodyOut1.body());
    assertNotNull(((SegmentedRequestData) bodyOut1).content());
    assertEquals(body1, ((SegmentedRequestData) bodyOut1).content());
    assertFalse(bodyOut1.endOfMessage());

    Request bodyOut2 = requests.remove(0);

    assertNotNull(bodyOut2);
    assertTrue(bodyOut2 instanceof SegmentedRequestData);
    assertEquals("h2", bodyOut2.version());
    assertEquals(HttpMethod.POST, bodyOut2.method());
    assertEquals("/", bodyOut2.path());
    assertFalse(bodyOut2.hasBody());
    assertNotNull(bodyOut2.body());
    assertNotNull(((SegmentedRequestData) bodyOut2).content());
    assertEquals(body2, ((SegmentedRequestData) bodyOut2).content());
    assertTrue(bodyOut2.endOfMessage());
  }

  @Test
  public void testStreamingRequestWithTrailingHeaders() {
    outputReceived = new CountDownLatch(4);

    Http2Headers headers = new DefaultHttp2Headers().method("POST").path("/");
    Http2Request requestIn = Http2Request.build(1, headers, false);
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    Http2Request content = Http2Request.build(1, new DefaultHttp2DataFrame(body1, false), false);
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    Http2Request lastContent = Http2Request.build(1, new DefaultHttp2DataFrame(body2, true), false);
    Http2Headers trailers = new DefaultHttp2Headers().set("foo", "bar");
    Http2Request lastHeaders = Http2Request.build(1, trailers, true);

    channel.writeInbound(requestIn);
    channel.writeInbound(content);
    channel.writeInbound(lastContent);
    channel.writeInbound(lastHeaders);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Request requestOut = requests.remove(0);

    assertNotNull(requestOut);
    assertTrue(requestOut instanceof SegmentedRequest);
    assertEquals("h2", requestOut.version());
    assertEquals(HttpMethod.POST, requestOut.method());
    assertEquals("/", requestOut.path());
    assertFalse(requestOut.hasBody());
    assertNotNull(requestOut.body());
    assertEquals(0, requestOut.body().readableBytes());

    Request bodyOut1 = requests.remove(0);

    assertNotNull(bodyOut1);
    assertTrue(bodyOut1 instanceof SegmentedRequestData);
    assertEquals("h2", bodyOut1.version());
    assertEquals(HttpMethod.POST, bodyOut1.method());
    assertEquals("/", bodyOut1.path());
    assertFalse(bodyOut1.hasBody());
    assertNotNull(bodyOut1.body());
    assertNotNull(((SegmentedRequestData) bodyOut1).content());
    assertEquals(body1, ((SegmentedRequestData) bodyOut1).content());
    assertFalse(bodyOut1.endOfMessage());

    Request bodyOut2 = requests.remove(0);

    assertNotNull(bodyOut2);
    assertTrue(bodyOut2 instanceof SegmentedRequestData);
    assertEquals("h2", bodyOut2.version());
    assertEquals(HttpMethod.POST, bodyOut2.method());
    assertEquals("/", bodyOut2.path());
    assertFalse(bodyOut2.hasBody());
    assertNotNull(bodyOut2.body());
    assertNotNull(((SegmentedRequestData) bodyOut2).content());
    assertEquals(body2, ((SegmentedRequestData) bodyOut2).content());
    assertFalse(bodyOut2.endOfMessage());

    Request trailersOut = requests.remove(0);

    assertNotNull(trailersOut);
    assertTrue(trailersOut instanceof SegmentedRequestData);
    assertEquals("h2", trailersOut.version());
    assertEquals(HttpMethod.POST, trailersOut.method());
    assertEquals("/", trailersOut.path());
    assertFalse(trailersOut.hasBody());
    assertNotNull(trailersOut.body());
    assertEquals(0, trailersOut.body().readableBytes());
    assertEquals(1, ((SegmentedRequestData) trailersOut).trailingHeaders().size());
    assertEquals("bar", ((SegmentedRequestData) trailersOut).trailingHeaders().get("foo"));
    assertTrue(trailersOut.endOfMessage());
  }

  @Test
  public void testFullResponse() throws Exception {
    outputReceived = new CountDownLatch(2);
    Http2Headers headersIn = new DefaultHttp2Headers().method("GET").path("/");
    Http2Request requestIn = Http2Request.build(1, headersIn, true);
    FullResponse responseIn =
        ResponseBuilders.newOk().streamId(1).body(Unpooled.EMPTY_BUFFER).build();

    channel.writeInbound(requestIn);
    channel.runPendingTasks(); // blocks
    channel.writeOutbound(responseIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Http2Response responseOut = responses.remove(0);

    assertNotNull(responseOut);
    assertTrue(responseOut.payload instanceof Http2Headers);
    assertEquals("200", ((Http2Headers) responseOut.payload).status().toString());
    assertTrue(responseOut.eos);
    assertEquals(1, responseOut.streamId);
  }

  @Test
  public void testFullResponseWithBody() throws Exception {
    outputReceived = new CountDownLatch(2);
    ByteBuf body = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "response");

    Http2Headers headersIn = new DefaultHttp2Headers().method("GET").path("/");
    Http2Request requestIn = Http2Request.build(1, headersIn, true);
    FullResponse responseIn = ResponseBuilders.newOk().streamId(1).body(body).build();

    channel.writeInbound(requestIn);
    channel.runPendingTasks(); // blocks
    channel.writeOutbound(responseIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Http2Response responseOut = responses.remove(0);

    assertNotNull(responseOut);
    assertTrue(responseOut.payload instanceof Http2Headers);
    assertEquals("200", ((Http2Headers) responseOut.payload).status().toString());
    assertFalse(responseOut.eos);
    assertEquals(1, responseOut.streamId);

    Http2Response bodyOut1 = responses.remove(0);

    assertNotNull(bodyOut1);
    assertTrue(bodyOut1.payload instanceof Http2DataFrame);
    assertEquals(body, ((Http2DataFrame) bodyOut1.payload).content());
    assertTrue(bodyOut1.eos);
    assertEquals(1, bodyOut1.streamId);
  }

  @Test
  public void testStreamingResponse() throws Exception {
    outputReceived = new CountDownLatch(3);
    Http2Headers headersIn = new DefaultHttp2Headers().method("GET").path("/");
    Http2Request requestIn = Http2Request.build(1, headersIn, true);

    SegmentedResponse responseIn =
        DefaultSegmentedResponse.builder()
            .streamId(1)
            .status(OK)
            .headers(new DefaultHeaders())
            .build();

    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    SegmentedData content =
        DefaultSegmentedData.builder().streamId(1).content(body1).endOfMessage(false).build();

    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    SegmentedData lastContent =
        DefaultSegmentedData.builder()
            .content(body2)
            .streamId(1)
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

    Http2Response responseOut = responses.remove(0);

    assertNotNull(responseOut);
    assertTrue(responseOut.payload instanceof Http2Headers);
    assertEquals("200", ((Http2Headers) responseOut.payload).status().toString());
    assertFalse(responseOut.eos);
    assertEquals(1, responseOut.streamId);

    Http2Response bodyOut1 = responses.remove(0);

    assertNotNull(bodyOut1);
    assertTrue(bodyOut1.payload instanceof Http2DataFrame);
    assertEquals(body1, ((Http2DataFrame) bodyOut1.payload).content());
    assertFalse(bodyOut1.eos);
    assertEquals(1, bodyOut1.streamId);

    Http2Response bodyOut2 = responses.remove(0);

    assertNotNull(bodyOut2);
    assertTrue(bodyOut2.payload instanceof Http2DataFrame);
    assertEquals(body2, ((Http2DataFrame) bodyOut2.payload).content());
    assertTrue(bodyOut2.eos);
    assertEquals(1, bodyOut2.streamId);
  }

  @Test
  public void testInterleavedStreamingMessages() throws Exception {
    outputReceived = new CountDownLatch(4);

    // given two streams
    int streamIdOne = 1;
    int streamIdTwo = 2;

    // given an h2 request
    Http2Request requestInitial1 =
        Http2Request.build(streamIdOne, new DefaultHttp2Headers().method("GET").path("/"), false);
    Http2Request requestSubsequential1 =
        Http2Request.build(streamIdOne, new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER), true);

    // given another h2 request
    Http2Request requestInitial2 =
        Http2Request.build(streamIdTwo, new DefaultHttp2Headers().method("POST").path("/"), false);
    Http2Request requestSubsequential2 =
        Http2Request.build(streamIdTwo, new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER), true);

    // given an h2 response
    Http2Response responseInitial1 =
        Http2Response.build(streamIdOne, new DefaultHttp2Headers().status(OK.codeAsText()));
    Http2Response responseSubSequential1 =
        Http2Response.build(
            streamIdOne,
            new DefaultHttp2DataFrame(
                ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1"), true),
            true);

    // given another h2 response
    Http2Response responseInitial2 =
        Http2Response.build(streamIdTwo, new DefaultHttp2Headers().status(CREATED.codeAsText()));
    Http2Response responseSubSequential2 =
        Http2Response.build(
            streamIdTwo,
            new DefaultHttp2DataFrame(
                ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2"), true),
            true);

    // when the 2 requests are interleaved
    channel.writeInbound(requestInitial1);
    channel.writeInbound(requestInitial2);
    channel.writeInbound(requestSubsequential1);
    channel.writeInbound(requestSubsequential2);
    channel.runPendingTasks(); // blocks

    // and responses are interleaved
    channel.runPendingTasks(); // blocks
    channel.writeOutbound(responseInitial1);
    channel.writeOutbound(responseInitial2);
    channel.writeOutbound(responseSubSequential1);
    channel.writeOutbound(responseSubSequential2);

    // then the first stream id responses should be correct
    {
      Http2Response headersOut = responses.get(0);
      Http2Response bodyOut = responses.get(2);

      assertNotNull(headersOut);
      assertTrue(headersOut.payload instanceof Http2Headers);
      assertEquals("200", ((Http2Headers) headersOut.payload).status().toString());
      assertFalse(headersOut.eos);
      assertEquals(streamIdOne, headersOut.streamId);

      assertNotNull(bodyOut);
      assertTrue(bodyOut.payload instanceof Http2DataFrame);
      assertTrue(bodyOut.eos);
      assertEquals(
          "body1", ((Http2DataFrame) bodyOut.payload).content().toString(CharsetUtil.UTF_8));
      assertEquals(streamIdOne, bodyOut.streamId);
    }

    // then the second stream id responses should be correct
    {
      Http2Response headersOut = responses.get(1);
      Http2Response bodyOut = responses.get(3);

      assertNotNull(headersOut);
      assertTrue(headersOut.payload instanceof Http2Headers);
      assertEquals("201", ((Http2Headers) headersOut.payload).status().toString());
      assertFalse(headersOut.eos);
      assertEquals(streamIdTwo, headersOut.streamId);

      assertNotNull(bodyOut);
      assertTrue(bodyOut.payload instanceof Http2DataFrame);
      assertTrue(bodyOut.eos);
      assertEquals(
          "body2", ((Http2DataFrame) bodyOut.payload).content().toString(CharsetUtil.UTF_8));
      assertEquals(streamIdTwo, bodyOut.streamId);
    }

    responses.clear();
  }

  @Test
  public void testStreamingResponseWithTrailingHeaders() {
    outputReceived = new CountDownLatch(3);
    Http2Headers headersIn = new DefaultHttp2Headers().method("GET").path("/");
    Http2Request requestIn = Http2Request.build(1, headersIn, true);

    SegmentedResponse responseIn =
        DefaultSegmentedResponse.builder()
            .status(OK)
            .streamId(1)
            .headers(new DefaultHeaders())
            .build();
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    SegmentedData content =
        DefaultSegmentedData.builder().streamId(1).content(body1).endOfMessage(false).build();
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    SegmentedData lastContent =
        DefaultSegmentedData.builder()
            .content(body2)
            .streamId(1)
            .endOfMessage(true)
            .trailingHeaders(new DefaultHeaders().set("foo", "bar"))
            .build();

    channel.writeInbound(requestIn);
    channel.runPendingTasks(); // blocks
    channel.writeOutbound(responseIn);
    channel.writeOutbound(content);
    channel.writeOutbound(lastContent);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Http2Response responseOut = responses.remove(0);

    assertNotNull(responseOut);
    assertTrue(responseOut.payload instanceof Http2Headers);
    assertEquals("200", ((Http2Headers) responseOut.payload).status().toString());
    assertFalse(responseOut.eos);
    assertEquals(1, responseOut.streamId);

    Http2Response bodyOut1 = responses.remove(0);

    assertNotNull(bodyOut1);
    assertTrue(bodyOut1.payload instanceof Http2DataFrame);
    assertEquals(body1, ((Http2DataFrame) bodyOut1.payload).content());
    assertFalse(bodyOut1.eos);
    assertEquals(1, bodyOut1.streamId);

    Http2Response bodyOut2 = responses.remove(0);

    assertNotNull(bodyOut2);
    assertTrue(bodyOut2.payload instanceof Http2DataFrame);
    assertEquals(body2, ((Http2DataFrame) bodyOut2.payload).content());
    assertFalse(bodyOut2.eos);
    assertEquals(1, bodyOut2.streamId);

    Http2Response trailersOut = responses.remove(0);

    assertNotNull(trailersOut);
    assertTrue(trailersOut.payload instanceof Http2Headers);
    assertEquals("bar", ((Http2Headers) trailersOut.payload).get("foo"));
    assertTrue(trailersOut.eos);
    assertEquals(1, trailersOut.streamId);
  }
}
