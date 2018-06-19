package com.xjeffrose.xio.http;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import helloworld.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class GrpcRequestHandlerTest extends Assert {

  private EmbeddedChannel channel;
  private GrpcRequestHandler subject;

  private ByteBuf bufferFor(com.google.protobuf.GeneratedMessageV3 protoObject) {
    byte[] dataBytes = protoObject.toByteArray();
    int length = dataBytes.length;
    byte[] lengthByteBuffer = ByteBuffer.allocate(4).putInt(length).array();
    byte[] compressedByteBuffer = ByteBuffer.allocate(1).put((byte)0).array();

    ByteBuf grpcRequestBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(length + 5, length + 5);

    grpcRequestBuffer.writeBytes(compressedByteBuffer);
    grpcRequestBuffer.writeBytes(lengthByteBuffer);
    grpcRequestBuffer.writeBytes(dataBytes);

    return grpcRequestBuffer;
  }

  private <T extends GeneratedMessageV3> T protoObjectFor(ByteBuf originalBuffer, GrpcRequestParser<T> parser) {
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

  @Before
  public void setUp() {
    subject = new GrpcRequestHandler<HelloRequest, HelloReply>(HelloRequest::parseFrom, (HelloRequest request) -> {
      return HelloReply.newBuilder().setMessage("I'm a reply " + request.getName()).build();
    });

    channel = new EmbeddedChannel(new SimpleChannelInboundHandler<Request>() {
      @Override
      protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
        subject.handle(ctx, request, null);
      }
    });
  }

  @Test
  public void test() {
    HelloRequest grpcRequest = HelloRequest.newBuilder().setName("myName").build();
    ByteBuf grpcRequestBuffer = bufferFor(grpcRequest);

    Request request = DefaultSegmentedRequest
      .builder()
      .path("/")
      .method(HttpMethod.GET)
      .headers(new DefaultHeaders())
      .build();
    DefaultSegmentedData requestData = DefaultSegmentedData
      .builder()
      .content(grpcRequestBuffer)
      .endOfMessage(true)
      .build();

    SegmentedRequestData segmentedRequest = new SegmentedRequestData(request, requestData);

    channel.writeInbound(segmentedRequest);

    // TODO brian: clean up assert
    HelloReply actualReply = protoObjectFor(channel.readOutbound(), HelloReply::parseFrom);
    HelloReply expectedReply = HelloReply.newBuilder().setMessage("I'm a reply " + "myName").build();
    assertEquals(actualReply, expectedReply);
  }
}
