package com.xjeffrose.xio.http;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import helloworld.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GrpcRequestHandlerTest extends Assert {

  private EmbeddedChannel channel;
  private GrpcRequestHandler subject;
  private static final String responsePrefix = "I'm a response: ";

  @Before
  public void setUp() {
    subject =
        new GrpcRequestHandler<>(
            HelloRequest::parseFrom,
            (HelloRequest request) ->
                HelloReply.newBuilder().setMessage(responsePrefix + request.getName()).build());

    channel =
        new EmbeddedChannel(
            new SimpleChannelInboundHandler<Request>() {
              @Override
              protected void channelRead0(ChannelHandlerContext ctx, Request request) {
                subject.handle(ctx, request, null);
              }
            });
  }

  @Test
  public void testGetAppLogic() {
    Function<HelloRequest, HelloReply> expectedAppLogic =
        (HelloRequest request) -> HelloReply.getDefaultInstance();
    GrpcRequestHandler handler =
        new GrpcRequestHandler<>(HelloRequest::parseFrom, expectedAppLogic);

    assertSame(expectedAppLogic, handler.getAppLogic());
  }

  @Test
  public void testSimpleRequest() {
    HelloRequest grpcRequest = HelloRequest.newBuilder().setName("myName").build();
    ByteBuf grpcRequestBuffer = bufferFor(grpcRequest);
    int streamId = 123;

    SegmentedRequestData segmentedRequest = fullGrpcRequest(grpcRequestBuffer, streamId, true);
    channel.writeInbound(segmentedRequest);

    Response response = channel.readOutbound();
    SegmentedData segmentedData = channel.readOutbound();

    assertEquals(HttpResponseStatus.OK, response.status());
    assertEquals(streamId, response.streamId());
    assertEquals("application/grpc+proto", response.headers().get(HttpHeaderNames.CONTENT_TYPE));

    HelloReply actualReply = protoObjectFor(segmentedData.content(), HelloReply::parseFrom);
    HelloReply expectedReply =
        HelloReply.newBuilder().setMessage(responsePrefix + grpcRequest.getName()).build();
    assertEquals(actualReply, expectedReply);

    assertEquals("0", Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-status"));
    assertFalse(Objects.requireNonNull(segmentedData.trailingHeaders()).contains("grpc-message"));
    assertEquals(streamId, segmentedData.streamId());
    assertTrue(segmentedData.endOfMessage());
  }

  @Test
  public void testChunkedRequest() {
    HelloRequest grpcRequest = HelloRequest.newBuilder().setName("myName").build();
    ByteBuf grpcRequestBuffer = bufferFor(grpcRequest);
    int streamId = 234;

    int middleIndex = grpcRequestBuffer.readableBytes() / 2;
    ByteBuf firstHalf = grpcRequestBuffer.slice(0, middleIndex);
    ByteBuf secondHalf =
        grpcRequestBuffer.slice(middleIndex, grpcRequestBuffer.readableBytes() - middleIndex);

    channel.writeInbound(fullGrpcRequest(firstHalf, streamId, false));
    channel.writeInbound(fullGrpcRequest(secondHalf, streamId, true));

    Response response = channel.readOutbound();
    SegmentedData segmentedData = channel.readOutbound();

    assertEquals(HttpResponseStatus.OK, response.status());
    assertEquals(streamId, response.streamId());
    assertEquals("application/grpc+proto", response.headers().get(HttpHeaderNames.CONTENT_TYPE));

    HelloReply actualReply = protoObjectFor(segmentedData.content(), HelloReply::parseFrom);
    HelloReply expectedReply =
        HelloReply.newBuilder().setMessage(responsePrefix + grpcRequest.getName()).build();
    assertEquals(actualReply, expectedReply);

    assertEquals("0", Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-status"));
    assertFalse(Objects.requireNonNull(segmentedData.trailingHeaders()).contains("grpc-message"));
    assertEquals(streamId, segmentedData.streamId());
    assertTrue(segmentedData.endOfMessage());
  }

  @Test
  public void testCompressedFlag() {
    HelloRequest grpcRequest = HelloRequest.newBuilder().setName("myName").build();
    ByteBuf grpcRequestBuffer = bufferFor(grpcRequest, true);
    int streamId = 345;

    SegmentedRequestData segmentedRequest = fullGrpcRequest(grpcRequestBuffer, streamId, true);
    channel.writeInbound(segmentedRequest);

    Response response = channel.readOutbound();
    SegmentedData segmentedData = channel.readOutbound();

    assertEquals(HttpResponseStatus.OK, response.status());
    assertEquals(streamId, response.streamId());
    assertEquals("application/grpc+proto", response.headers().get(HttpHeaderNames.CONTENT_TYPE));

    assertEquals("12", Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-status"));
    String actualMessage =
        grpcDecodedString(
            Objects.requireNonNull(
                Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-message")));
    assertEquals("compression not supported", actualMessage);
    assertEquals(streamId, segmentedData.streamId());
    assertTrue(segmentedData.endOfMessage());
  }

  @Test
  public void testIndicatedSizeTooLarge() {
    byte[] lengthByteBuffer = ByteBuffer.allocate(4).putInt(1_000_001).array();
    byte[] compressedByteBuffer = ByteBuffer.allocate(1).put((byte) 0).array();
    int streamId = 345;

    ByteBuf grpcRequestBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(5, 5);
    grpcRequestBuffer.writeBytes(compressedByteBuffer);
    grpcRequestBuffer.writeBytes(lengthByteBuffer);

    channel.writeInbound(fullGrpcRequest(grpcRequestBuffer, streamId, true));

    Response response = channel.readOutbound();
    SegmentedData segmentedData = channel.readOutbound();

    assertEquals(HttpResponseStatus.OK, response.status());
    assertEquals(streamId, response.streamId());
    assertEquals("application/grpc+proto", response.headers().get(HttpHeaderNames.CONTENT_TYPE));

    assertEquals("8", Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-status"));
    String actualMessage =
        grpcDecodedString(
            Objects.requireNonNull(
                Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-message")));
    assertEquals("payload is too large", actualMessage);
    assertEquals(streamId, segmentedData.streamId());
    assertTrue(segmentedData.endOfMessage());
  }

  @Test
  public void testDidNotGetAllMetaData() {
    byte[] lengthByteBuffer = ByteBuffer.allocate(4).putInt(0).array();
    int streamId = 456;

    ByteBuf grpcRequestBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(4, 4);
    grpcRequestBuffer.writeBytes(lengthByteBuffer);

    channel.writeInbound(fullGrpcRequest(grpcRequestBuffer, streamId, true));

    Response response = channel.readOutbound();
    SegmentedData segmentedData = channel.readOutbound();

    assertEquals(HttpResponseStatus.OK, response.status());
    assertEquals(streamId, response.streamId());
    assertEquals("application/grpc+proto", response.headers().get(HttpHeaderNames.CONTENT_TYPE));

    assertEquals("13", Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-status"));
    String actualMessage =
        grpcDecodedString(
            Objects.requireNonNull(
                Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-message")));
    assertEquals("metadata not provided", actualMessage);
    assertEquals(streamId, segmentedData.streamId());
    assertTrue(segmentedData.endOfMessage());
  }

  @Test
  public void testIndicatedSizeSmallerThanActualSizeSingleChunk() {
    HelloRequest grpcRequest = HelloRequest.newBuilder().setName("myName").build();
    byte[] dataBytes = grpcRequest.toByteArray();
    byte[] lengthByteBuffer = ByteBuffer.allocate(4).putInt(2).array();
    byte[] compressedByteBuffer = ByteBuffer.allocate(1).put((byte) 0).array();
    int streamId = 567;

    int length = dataBytes.length;
    ByteBuf grpcRequestBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(length + 5, length + 5);

    grpcRequestBuffer.writeBytes(compressedByteBuffer);
    grpcRequestBuffer.writeBytes(lengthByteBuffer);
    grpcRequestBuffer.writeBytes(dataBytes);

    channel.writeInbound(fullGrpcRequest(grpcRequestBuffer, streamId, true));

    Response response = channel.readOutbound();
    SegmentedData segmentedData = channel.readOutbound();

    assertEquals(HttpResponseStatus.OK, response.status());
    assertEquals(streamId, response.streamId());
    assertEquals("application/grpc+proto", response.headers().get(HttpHeaderNames.CONTENT_TYPE));

    assertEquals("13", Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-status"));
    String actualMessage =
        grpcDecodedString(
            Objects.requireNonNull(
                Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-message")));
    assertEquals("indicated payload size does not match actual payload size", actualMessage);
    assertEquals(streamId, segmentedData.streamId());
    assertTrue(segmentedData.endOfMessage());
  }

  @Test
  public void testIndicatedSizeSmallerThanActualSizeMultipleChunks() {
    HelloRequest grpcRequest = HelloRequest.newBuilder().setName("myName").build();
    byte[] dataBytes = grpcRequest.toByteArray();
    byte[] lengthByteBuffer = ByteBuffer.allocate(4).putInt(6).array();
    byte[] compressedByteBuffer = ByteBuffer.allocate(1).put((byte) 0).array();
    int streamId = 567;

    int length = dataBytes.length;
    ByteBuf grpcRequestBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(length + 5, length + 5);

    grpcRequestBuffer.writeBytes(compressedByteBuffer);
    grpcRequestBuffer.writeBytes(lengthByteBuffer);
    grpcRequestBuffer.writeBytes(dataBytes);

    int middleIndex = grpcRequestBuffer.readableBytes() / 2;
    ByteBuf firstHalf = grpcRequestBuffer.slice(0, middleIndex);
    ByteBuf secondHalf =
        grpcRequestBuffer.slice(middleIndex, grpcRequestBuffer.readableBytes() - middleIndex);

    channel.writeInbound(fullGrpcRequest(firstHalf, streamId, false));
    channel.writeInbound(fullGrpcRequest(secondHalf, streamId, true));

    Response response = channel.readOutbound();
    SegmentedData segmentedData = channel.readOutbound();

    assertEquals(HttpResponseStatus.OK, response.status());
    assertEquals(streamId, response.streamId());
    assertEquals("application/grpc+proto", response.headers().get(HttpHeaderNames.CONTENT_TYPE));

    assertEquals("13", Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-status"));
    String actualMessage =
        grpcDecodedString(
            Objects.requireNonNull(
                Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-message")));
    assertEquals("indicated payload size does not match actual payload size", actualMessage);
    assertEquals(streamId, segmentedData.streamId());
    assertTrue(segmentedData.endOfMessage());
  }

  @Test
  public void testIndicatedSizeLargerThanActualSize() {
    HelloRequest grpcRequest = HelloRequest.newBuilder().setName("myName").build();
    byte[] dataBytes = grpcRequest.toByteArray();
    byte[] lengthByteBuffer = ByteBuffer.allocate(4).putInt(900_000).array();
    byte[] compressedByteBuffer = ByteBuffer.allocate(1).put((byte) 0).array();
    int streamId = 567;

    int length = dataBytes.length;
    ByteBuf grpcRequestBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(length + 5, length + 5);

    grpcRequestBuffer.writeBytes(compressedByteBuffer);
    grpcRequestBuffer.writeBytes(lengthByteBuffer);
    grpcRequestBuffer.writeBytes(dataBytes);

    channel.writeInbound(fullGrpcRequest(grpcRequestBuffer, streamId, true));

    Response response = channel.readOutbound();
    SegmentedData segmentedData = channel.readOutbound();

    assertEquals(HttpResponseStatus.OK, response.status());
    assertEquals(streamId, response.streamId());
    assertEquals("application/grpc+proto", response.headers().get(HttpHeaderNames.CONTENT_TYPE));

    assertEquals("13", Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-status"));
    String actualMessage =
        grpcDecodedString(
            Objects.requireNonNull(
                Objects.requireNonNull(segmentedData.trailingHeaders()).get("grpc-message")));
    assertEquals("indicated payload size does not match actual payload size", actualMessage);
    assertEquals(streamId, segmentedData.streamId());
    assertTrue(segmentedData.endOfMessage());
  }

  private ByteBuf bufferFor(
      com.google.protobuf.GeneratedMessageV3 protoObject, boolean compressed) {
    byte[] dataBytes = protoObject.toByteArray();
    int length = dataBytes.length;
    byte[] lengthByteBuffer = ByteBuffer.allocate(4).putInt(length).array();
    int compressedFlag = compressed ? 1 : 0;
    byte[] compressedByteBuffer = ByteBuffer.allocate(1).put((byte) compressedFlag).array();

    ByteBuf grpcRequestBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(length + 5, length + 5);

    grpcRequestBuffer.writeBytes(compressedByteBuffer);
    grpcRequestBuffer.writeBytes(lengthByteBuffer);
    grpcRequestBuffer.writeBytes(dataBytes);

    return grpcRequestBuffer;
  }

  private ByteBuf bufferFor(com.google.protobuf.GeneratedMessageV3 protoObject) {
    return bufferFor(protoObject, false);
  }

  private <T extends GeneratedMessageV3> T protoObjectFor(
      ByteBuf originalBuffer, GrpcRequestParser<T> parser) {
    int size = originalBuffer.slice(1, 4).readInt();
    ByteBuf buffer = UnpooledByteBufAllocator.DEFAULT.buffer(size, size);
    buffer.writeBytes(originalBuffer.slice(5, size));

    try {
      return parser.parse(buffer.nioBuffer());
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private SegmentedRequestData fullGrpcRequest(
      ByteBuf grpcRequestBuffer, int streamId, boolean endOfMessage) {
    Request request =
        DefaultSegmentedRequest.builder()
            .path("/")
            .method(HttpMethod.GET)
            .headers(new DefaultHeaders())
            .streamId(streamId)
            .build();
    DefaultSegmentedData requestData =
        DefaultSegmentedData.builder()
            .content(grpcRequestBuffer)
            .endOfMessage(endOfMessage)
            .streamId(streamId)
            .build();

    return new SegmentedRequestData(request, requestData);
  }

  private String grpcDecodedString(String input) {
    StringBuilder output = new StringBuilder();

    for (String hex : input.split("%")) {
      if (!"".equals(hex)) output.append((char) Integer.parseInt(hex, 16));
    }
    return output.toString();
  }
}
