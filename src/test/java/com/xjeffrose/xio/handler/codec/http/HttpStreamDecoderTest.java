package com.xjeffrose.xio.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderResultProvider;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;
import java.util.List;
import org.junit.Test;

import static io.netty.util.ReferenceCountUtil.releaseLater;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HttpStreamDecoderTest {

  @Test
  public void testAggregate() {
    HttpStreamDecoder aggr = new HttpStreamDecoder(1024 * 1024);
    EmbeddedChannel embedder = new EmbeddedChannel(aggr);

    HttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
      "http://localhost");
    message.headers().set("X-Test", true);
    HttpContent chunk1 = new DefaultHttpContent(
      Unpooled.copiedBuffer("test", CharsetUtil.US_ASCII));
    HttpContent chunk2 = new DefaultHttpContent(
      Unpooled.copiedBuffer("test2", CharsetUtil.US_ASCII));
    HttpContent chunk3 = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
    assertFalse(embedder.writeInbound(message));
    assertFalse(embedder.writeInbound(chunk1));
    assertFalse(embedder.writeInbound(chunk2));

    // this should trigger a channelRead event so return true
    assertTrue(embedder.writeInbound(chunk3));
    assertTrue(embedder.finish());
    FullHttpRequest aggregatedMessage = embedder.readInbound();
    assertNotNull(aggregatedMessage);
    assertEquals(chunk1.refCnt(), 1);
    assertEquals(chunk2.refCnt(), 1);
    assertEquals(chunk3.refCnt(), 1);

    assertEquals(chunk1.content().readableBytes() + chunk2.content().readableBytes(),
      HttpUtil.getContentLength(aggregatedMessage));
    assertEquals(aggregatedMessage.headers().get("X-Test"), Boolean.TRUE.toString());
    checkContentBuffer(aggregatedMessage);
    assertNull(embedder.readInbound());
  }

  private static void checkContentBuffer(FullHttpRequest aggregatedMessage) {
    CompositeByteBuf buffer = (CompositeByteBuf) aggregatedMessage.content();
    assertEquals(2, buffer.numComponents());
    List<ByteBuf> buffers = buffer.decompose(0, buffer.capacity());
    assertEquals(2, buffers.size());
    for (ByteBuf buf : buffers) {
      // This should be false as we decompose the buffer before to not have deep hierarchy
      assertFalse(buf instanceof CompositeByteBuf);
    }
    aggregatedMessage.release();
  }

  @Test
  public void testAggregateWithTrailer() {
    HttpStreamDecoder aggr = new HttpStreamDecoder(1024 * 1024);
    EmbeddedChannel embedder = new EmbeddedChannel(aggr);
    HttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
      "http://localhost");
    message.headers().set("X-Test", true);
    HttpUtil.setTransferEncodingChunked(message, true);
    HttpContent chunk1 = new DefaultHttpContent(
      Unpooled.copiedBuffer("test", CharsetUtil.US_ASCII));
    HttpContent chunk2 = new DefaultHttpContent(
      Unpooled.copiedBuffer("test2", CharsetUtil.US_ASCII));
    LastHttpContent trailer = new DefaultLastHttpContent();
    trailer.trailingHeaders().set("X-Trailer", true);

    assertFalse(embedder.writeInbound(message));
    assertFalse(embedder.writeInbound(chunk1));
    assertFalse(embedder.writeInbound(chunk2));

    // this should trigger a channelRead event so return true
    assertTrue(embedder.writeInbound(trailer));
    assertTrue(embedder.finish());
    FullHttpRequest aggregatedMessage = embedder.readInbound();
    assertNotNull(aggregatedMessage);
    assertEquals(chunk1.refCnt(), 1);
    assertEquals(chunk2.refCnt(), 1);

    assertEquals(chunk1.content().readableBytes() + chunk2.content().readableBytes(),
      HttpUtil.getContentLength(aggregatedMessage));
    assertEquals(aggregatedMessage.headers().get("X-Test"), Boolean.TRUE.toString());
    assertEquals(aggregatedMessage.trailingHeaders().get("X-Trailer"), Boolean.TRUE.toString());
    checkContentBuffer(aggregatedMessage);
    assertNull(embedder.readInbound());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidConstructorUsage() {
    new HttpStreamDecoder(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidMaxCumulationBufferComponents() {
    HttpStreamDecoder aggr = new HttpStreamDecoder(Integer.MAX_VALUE);
    aggr.setMaxCumulationBufferComponents(1);
  }

  @Test
  public void testAggregateTransferEncodingChunked() {
    HttpStreamDecoder aggr = new HttpStreamDecoder(1024 * 1024);
    EmbeddedChannel embedder = new EmbeddedChannel(aggr);

    HttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT,
      "http://localhost");
    message.headers().set("X-Test", true);
    message.headers().set("Transfer-Encoding", "Chunked");
    HttpContent chunk1 = new DefaultHttpContent(
      Unpooled.copiedBuffer("test", CharsetUtil.US_ASCII));
    HttpContent chunk2 = new DefaultHttpContent(
      Unpooled.copiedBuffer("test2", CharsetUtil.US_ASCII));
    HttpContent chunk3 = LastHttpContent.EMPTY_LAST_CONTENT;
    assertFalse(embedder.writeInbound(message));
    assertFalse(embedder.writeInbound(chunk1));
    assertFalse(embedder.writeInbound(chunk2));

    // this should trigger a channelRead event so return true
    assertTrue(embedder.writeInbound(chunk3));
    assertTrue(embedder.finish());
    FullHttpRequest aggregatedMessage = embedder.readInbound();
    assertNotNull(aggregatedMessage);

    assertEquals(chunk1.content().readableBytes() + chunk2.content().readableBytes(),
      HttpUtil.getContentLength(aggregatedMessage));
    assertEquals(aggregatedMessage.headers().get("X-Test"), Boolean.TRUE.toString());
    assertFalse(HttpUtil.isTransferEncodingChunked(aggregatedMessage));
    checkContentBuffer(aggregatedMessage);
    assertNull(embedder.readInbound());
  }

  @Test
  public void testBadRequest() {
    EmbeddedChannel ch = new EmbeddedChannel(new HttpRequestDecoder(),
      new HttpStreamDecoder(1024 * 1024));
    ch.writeInbound(Unpooled.copiedBuffer("GET / HTTP/1.0 with extra\r\n", CharsetUtil.UTF_8));
    Object inbound = ch.readInbound();
    assertTrue(inbound instanceof FullHttpRequest);
    assertTrue(((DecoderResultProvider) inbound).decoderResult().isFailure());
    assertNull(ch.readInbound());
    ch.finish();
  }

  @Test
  public void testBadResponse() throws Exception {
    EmbeddedChannel ch = new EmbeddedChannel(new HttpResponseDecoder(),
      new HttpStreamDecoder(1024 * 1024));
    ch.writeInbound(Unpooled.copiedBuffer("HTTP/1.0 BAD_CODE Bad Server\r\n", CharsetUtil.UTF_8));
    Object inbound = ch.readInbound();
    assertTrue(inbound instanceof FullHttpResponse);
    assertTrue(((DecoderResultProvider) inbound).decoderResult().isFailure());
    assertNull(ch.readInbound());
    ch.finish();
  }

  @Test
  public void testOversizedRequestWith100Continue() {
    EmbeddedChannel embedder = new EmbeddedChannel(new HttpStreamDecoder(8));

    // send an oversized request with 100 continue
    HttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT,
      "http://localhost");
    HttpUtil.set100ContinueExpected(message, true);
    HttpUtil.setContentLength(message, 16);

    HttpContent chunk1 =
      releaseLater(new DefaultHttpContent(Unpooled.copiedBuffer("some1234", CharsetUtil.US_ASCII)));
    HttpContent chunk2 =
      releaseLater(new DefaultHttpContent(Unpooled.copiedBuffer("test1234", CharsetUtil.US_ASCII)));
    HttpContent chunk3 = LastHttpContent.EMPTY_LAST_CONTENT;

    // Send a request with 100-continue + large Content-Length header value.
    assertFalse(embedder.writeInbound(message));

    // The aggregator should respond with '417.'
    FullHttpResponse response = (FullHttpResponse) embedder.readOutbound();
    assertEquals(response.status(), HttpResponseStatus.CONTINUE);

    // An well-behaving client could continue to send data
    assertFalse(embedder.writeInbound(chunk1));

    // The aggregator should not close the connection because keep-alive is on.
    assertTrue(embedder.isOpen());

    // client sending more data
    assertTrue(embedder.writeInbound(chunk2));
    assertTrue(embedder.writeInbound(chunk3));

    assertEquals(embedder.inboundMessages().remove(), message);
    assertEquals(embedder.inboundMessages().remove(), chunk1);
    assertEquals(embedder.inboundMessages().remove(), chunk2);
    assertEquals(embedder.inboundMessages().remove(), chunk3);

    assertEquals(chunk1.refCnt(), 1);
    assertEquals(chunk2.refCnt(), 1);
    assertEquals(chunk3.refCnt(), 1);

    assertFalse(embedder.finish());
  }

  @Test
  public void testOversizedRequestWith100ContinueAndDecoder() {
    EmbeddedChannel embedder = new EmbeddedChannel(new HttpRequestDecoder(),
      new HttpStreamDecoder(4));
    embedder.writeInbound(Unpooled.copiedBuffer("PUT /upload HTTP/1.1\r\n" +
      "Expect: 100-continue\r\n" +
      "Content-Length: 10\r\n\r\n", CharsetUtil.US_ASCII));

    assertNull(embedder.readInbound());

    FullHttpResponse response = embedder.readOutbound();
    assertEquals(response.getStatus(), HttpResponseStatus.CONTINUE);

    embedder.writeInbound(Unpooled.copiedBuffer("1234567890\r\n", CharsetUtil.US_ASCII));

    assertNotNull(embedder.inboundMessages().remove());
    assertNotNull(embedder.inboundMessages().remove());
    assertNull(embedder.readInbound());

    // Keep-alive is on by default in HTTP/1.1, so the connection should be still alive.
    assertTrue(embedder.isOpen());

    // The decoder should be reset by the aggregator at this point and be able to decode the next request.
    embedder.writeInbound(
      Unpooled.copiedBuffer("GET /max-upload-size HTTP/1.1\r\n\r\n", CharsetUtil.US_ASCII));

    FullHttpRequest request = (FullHttpRequest) embedder.readInbound();
    assertEquals(request.method(), HttpMethod.GET);
    assertEquals(request.uri(), "/max-upload-size");
    assertEquals(request.content().readableBytes(), 0);
    request.release();

    assertFalse(embedder.finish());
  }

  @Test
  public void testRequestAfterOversized100ContinueAndDecoder() {
    EmbeddedChannel embedder = new EmbeddedChannel(new HttpRequestDecoder(),
      new HttpStreamDecoder(15));

    // Write first request with Expect: 100-continue
    HttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT,
      "http://localhost");
    HttpUtil.set100ContinueExpected(message, true);
    HttpUtil.setContentLength(message, 16);

    HttpContent chunk1 =
      releaseLater(new DefaultHttpContent(Unpooled.copiedBuffer("sometest", CharsetUtil.US_ASCII)));
    HttpContent chunk2 =
      releaseLater(new DefaultHttpContent(Unpooled.copiedBuffer("sometest", CharsetUtil.US_ASCII)));
    HttpContent chunk3 = LastHttpContent.EMPTY_LAST_CONTENT;

    // Send a request with 100-continue + large Content-Length header value.
    assertFalse(embedder.writeInbound(message));

    // The aggregator should respond with '417'
    FullHttpResponse response = (FullHttpResponse) embedder.readOutbound();
    assertEquals(response.status(), HttpResponseStatus.CONTINUE);

    // The aggregator should not close the connection because keep-alive is on.
    assertTrue(embedder.isOpen());

    assertFalse(embedder.writeInbound(chunk1));
    assertTrue(embedder.writeInbound(chunk2));
    assertTrue(embedder.writeInbound(chunk3));

    assertEquals(embedder.inboundMessages().remove(), message);
    assertEquals(embedder.inboundMessages().remove(), chunk1);
    assertEquals(embedder.inboundMessages().remove(), chunk2);
    assertEquals(embedder.inboundMessages().remove(), chunk3);

    assertEquals(chunk1.refCnt(), 1);
    assertEquals(chunk2.refCnt(), 1);
    assertEquals(chunk3.refCnt(), 1);

    assertFalse(embedder.finish());
  }

  @Test
  public void testOversizedRequest() {
    EmbeddedChannel embedder = new EmbeddedChannel(new HttpStreamDecoder(4));
    HttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT,
      "http://localhost");
    HttpUtil.setTransferEncodingChunked(message, true);
    HttpContent chunk1 = new DefaultHttpContent(
      Unpooled.copiedBuffer("test", CharsetUtil.US_ASCII));
    HttpContent chunk2 = new DefaultHttpContent(
      Unpooled.copiedBuffer("test2", CharsetUtil.US_ASCII));
    HttpContent chunk3 = LastHttpContent.EMPTY_LAST_CONTENT;

    assertFalse(embedder.writeInbound(message));
    assertFalse(embedder.writeInbound(chunk1));
    assertTrue(embedder.writeInbound(chunk2));
    assertTrue(embedder.writeInbound(chunk3));

    assertEquals(embedder.inboundMessages().remove(), message);
    assertEquals(embedder.inboundMessages().remove(), chunk1);
    assertEquals(embedder.inboundMessages().remove(), chunk2);
    assertEquals(embedder.inboundMessages().remove(), chunk3);

    assertTrue(HttpUtil.isTransferEncodingChunked(message));

    assertEquals(chunk1.refCnt(), 1);
    assertEquals(chunk2.refCnt(), 1);
    assertEquals(chunk3.refCnt(), 1);
  }

  @Test
  public void testOversizedRequestWithoutKeepAlive() {
    // send a HTTP/1.0 request with no keep-alive header
    HttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.PUT,
      "http://localhost");
    HttpUtil.setContentLength(message, 5);
    checkOversizedRequest(message);
  }

  @Test
  public void testOversizedRequestWithContentLength() {
    HttpRequest message = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT,
      "http://localhost");
    HttpUtil.setContentLength(message, 5);
    checkOversizedRequest(message);
  }

  private static void checkOversizedRequest(HttpRequest message) {
    EmbeddedChannel embedder = new EmbeddedChannel(new HttpStreamDecoder(4));

    assertTrue(embedder.writeInbound(message));
    assertEquals(embedder.inboundMessages().remove(), message);
  }
}
