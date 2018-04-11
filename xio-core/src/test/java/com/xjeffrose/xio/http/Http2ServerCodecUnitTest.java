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

    assertTrue(requestOut != null);
    assertTrue(requestOut instanceof FullRequest);
    assertEquals("h2", requestOut.version());
    assertEquals(HttpMethod.GET, requestOut.method());
    assertEquals("/", requestOut.path());
    assertFalse(requestOut.hasBody());
    assertFalse(requestOut.body() == null);
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

    assertTrue(requestOut != null);
    assertTrue(requestOut instanceof StreamingRequest);
    assertEquals("h2", requestOut.version());
    assertEquals(HttpMethod.POST, requestOut.method());
    assertEquals("/", requestOut.path());
    assertFalse(requestOut.hasBody());
    assertFalse(requestOut.body() == null);
    assertEquals(0, requestOut.body().readableBytes());

    Request bodyOut1 = requests.remove(0);

    assertTrue(bodyOut1 != null);
    assertTrue(bodyOut1 instanceof StreamingRequestData);
    assertEquals("h2", bodyOut1.version());
    assertEquals(HttpMethod.POST, bodyOut1.method());
    assertEquals("/", bodyOut1.path());
    assertFalse(bodyOut1.hasBody());
    assertFalse(bodyOut1.body() == null);
    assertFalse(((StreamingRequestData) bodyOut1).content() == null);
    assertEquals(body1, ((StreamingRequestData) bodyOut1).content());
    assertFalse(((StreamingRequestData) bodyOut1).endOfMessage());

    Request bodyOut2 = requests.remove(0);

    assertTrue(bodyOut2 != null);
    assertTrue(bodyOut2 instanceof StreamingRequestData);
    assertEquals("h2", bodyOut2.version());
    assertEquals(HttpMethod.POST, bodyOut2.method());
    assertEquals("/", bodyOut2.path());
    assertFalse(bodyOut2.hasBody());
    assertFalse(bodyOut2.body() == null);
    assertFalse(((StreamingRequestData) bodyOut2).content() == null);
    assertEquals(body2, ((StreamingRequestData) bodyOut2).content());
    assertTrue(((StreamingRequestData) bodyOut2).endOfMessage());
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

    assertTrue(requestOut != null);
    assertTrue(requestOut instanceof StreamingRequest);
    assertEquals("h2", requestOut.version());
    assertEquals(HttpMethod.POST, requestOut.method());
    assertEquals("/", requestOut.path());
    assertFalse(requestOut.hasBody());
    assertFalse(requestOut.body() == null);
    assertEquals(0, requestOut.body().readableBytes());

    Request bodyOut1 = requests.remove(0);

    assertTrue(bodyOut1 != null);
    assertTrue(bodyOut1 instanceof StreamingRequestData);
    assertEquals("h2", bodyOut1.version());
    assertEquals(HttpMethod.POST, bodyOut1.method());
    assertEquals("/", bodyOut1.path());
    assertFalse(bodyOut1.hasBody());
    assertFalse(bodyOut1.body() == null);
    assertFalse(((StreamingRequestData) bodyOut1).content() == null);
    assertEquals(body1, ((StreamingRequestData) bodyOut1).content());
    assertFalse(((StreamingRequestData) bodyOut1).endOfMessage());

    Request bodyOut2 = requests.remove(0);

    assertTrue(bodyOut2 != null);
    assertTrue(bodyOut2 instanceof StreamingRequestData);
    assertEquals("h2", bodyOut2.version());
    assertEquals(HttpMethod.POST, bodyOut2.method());
    assertEquals("/", bodyOut2.path());
    assertFalse(bodyOut2.hasBody());
    assertFalse(bodyOut2.body() == null);
    assertFalse(((StreamingRequestData) bodyOut2).content() == null);
    assertEquals(body2, ((StreamingRequestData) bodyOut2).content());
    assertFalse(((StreamingRequestData) bodyOut2).endOfMessage());

    Request trailersOut = requests.remove(0);

    assertTrue(trailersOut != null);
    assertTrue(trailersOut instanceof StreamingRequestData);
    assertEquals("h2", trailersOut.version());
    assertEquals(HttpMethod.POST, trailersOut.method());
    assertEquals("/", trailersOut.path());
    assertFalse(trailersOut.hasBody());
    assertFalse(trailersOut.body() == null);
    assertEquals(0, trailersOut.body().readableBytes());
    assertEquals(1, ((StreamingRequestData) trailersOut).trailingHeaders().size());
    assertEquals("bar", ((StreamingRequestData) trailersOut).trailingHeaders().get("foo"));
    assertTrue(((StreamingRequestData) trailersOut).endOfMessage());
  }

  @Test
  public void testFullResponse() throws Exception {
    outputReceived = new CountDownLatch(2);
    Http2Headers headersIn = new DefaultHttp2Headers().method("GET").path("/");
    Http2Request requestIn = Http2Request.build(1, headersIn, true);
    FullResponse responseIn = ResponseBuilders.newOk().body(Unpooled.EMPTY_BUFFER).build();

    channel.writeInbound(requestIn);
    channel.runPendingTasks(); // blocks
    channel.writeOutbound(responseIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Http2Response responseOut = (Http2Response) responses.remove(0);

    assertTrue(responseOut != null);
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
    FullResponse responseIn = ResponseBuilders.newOk().body(body).build();

    channel.writeInbound(requestIn);
    channel.runPendingTasks(); // blocks
    channel.writeOutbound(responseIn);

    channel.runPendingTasks(); // blocks

    Uninterruptibles.awaitUninterruptibly(outputReceived);

    Http2Response responseOut = (Http2Response) responses.remove(0);

    assertTrue(responseOut != null);
    assertTrue(responseOut.payload instanceof Http2Headers);
    assertEquals("200", ((Http2Headers) responseOut.payload).status().toString());
    assertFalse(responseOut.eos);
    assertEquals(1, responseOut.streamId);

    Http2Response bodyOut1 = (Http2Response) responses.remove(0);

    assertTrue(bodyOut1 != null);
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

    StreamingResponse responseIn =
        DefaultStreamingResponse.builder().status(OK).headers(new DefaultHeaders()).build();
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    StreamingData content =
        DefaultStreamingData.builder().content(body1).endOfMessage(false).build();
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    StreamingData lastContent =
        DefaultStreamingData.builder()
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

    Http2Response responseOut = (Http2Response) responses.remove(0);

    assertTrue(responseOut != null);
    assertTrue(responseOut.payload instanceof Http2Headers);
    assertEquals("200", ((Http2Headers) responseOut.payload).status().toString());
    assertFalse(responseOut.eos);
    assertEquals(1, responseOut.streamId);

    Http2Response bodyOut1 = (Http2Response) responses.remove(0);

    assertTrue(bodyOut1 != null);
    assertTrue(bodyOut1.payload instanceof Http2DataFrame);
    assertEquals(body1, ((Http2DataFrame) bodyOut1.payload).content());
    assertFalse(bodyOut1.eos);
    assertEquals(1, bodyOut1.streamId);

    Http2Response bodyOut2 = (Http2Response) responses.remove(0);

    assertTrue(bodyOut2 != null);
    assertTrue(bodyOut2.payload instanceof Http2DataFrame);
    assertEquals(body2, ((Http2DataFrame) bodyOut2.payload).content());
    assertTrue(bodyOut2.eos);
    assertEquals(1, bodyOut2.streamId);
  }

  @Test
  public void testStreamingResponseWithTrailingHeaders() {
    outputReceived = new CountDownLatch(3);
    Http2Headers headersIn = new DefaultHttp2Headers().method("GET").path("/");
    Http2Request requestIn = Http2Request.build(1, headersIn, true);

    StreamingResponse responseIn =
        DefaultStreamingResponse.builder().status(OK).headers(new DefaultHeaders()).build();
    ByteBuf body1 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body1");
    StreamingData content =
        DefaultStreamingData.builder().content(body1).endOfMessage(false).build();
    ByteBuf body2 = ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, "body2");
    StreamingData lastContent =
        DefaultStreamingData.builder()
            .content(body2)
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

    Http2Response responseOut = (Http2Response) responses.remove(0);

    assertTrue(responseOut != null);
    assertTrue(responseOut.payload instanceof Http2Headers);
    assertEquals("200", ((Http2Headers) responseOut.payload).status().toString());
    assertFalse(responseOut.eos);
    assertEquals(1, responseOut.streamId);

    Http2Response bodyOut1 = (Http2Response) responses.remove(0);

    assertTrue(bodyOut1 != null);
    assertTrue(bodyOut1.payload instanceof Http2DataFrame);
    assertEquals(body1, ((Http2DataFrame) bodyOut1.payload).content());
    assertFalse(bodyOut1.eos);
    assertEquals(1, bodyOut1.streamId);

    Http2Response bodyOut2 = (Http2Response) responses.remove(0);

    assertTrue(bodyOut2 != null);
    assertTrue(bodyOut2.payload instanceof Http2DataFrame);
    assertEquals(body2, ((Http2DataFrame) bodyOut2.payload).content());
    assertFalse(bodyOut2.eos);
    assertEquals(1, bodyOut2.streamId);

    Http2Response trailersOut = responses.remove(0);

    assertTrue(trailersOut != null);
    assertTrue(trailersOut.payload instanceof Http2Headers);
    assertEquals("bar", ((Http2Headers) trailersOut.payload).get("foo"));
    assertTrue(trailersOut.eos);
    assertEquals(1, trailersOut.streamId);
  }
}
