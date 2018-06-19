package com.xjeffrose.xio.http;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.function.Function;

public class GrpcRequestHandler<GrpcRequest extends
  com.google.protobuf.GeneratedMessageV3, GrpcResponse extends com.google.protobuf.GeneratedMessageV3> implements PipelineRequestHandler {
  private static class GrpcState {
    int size;
    ByteBuf buffer;

    GrpcState(int size, ByteBuf buffer) {
      this.size = size;
      this.buffer = buffer;
    }
  }

  private static final AttributeKey<HashMap<Integer, GrpcState>> CHANNEL_MESSAGE_SESSION_KEY =
    AttributeKey.newInstance("xio_grpc_session");

  private static final int sizeTooLargeToHandle = 1_000_000;

  static HashMap<Integer, GrpcState> lazyCreateSession(ChannelHandlerContext ctx) {
    HashMap<Integer, GrpcState> session = ctx.channel().attr(CHANNEL_MESSAGE_SESSION_KEY).get();
    if (session == null) {
      session = new HashMap<Integer, GrpcState>();
      ctx.channel().attr(CHANNEL_MESSAGE_SESSION_KEY).set(session);
    }
    return session;
  }

  private final GrpcRequestParser<GrpcRequest> requestParser;
  private final Function<GrpcRequest, GrpcResponse> appLogic;

  // TODO brian: make this pretty, be able to pass in HelloRequest::parseFrom
  GrpcRequestHandler(GrpcRequestParser<GrpcRequest> requestParser, Function<GrpcRequest, GrpcResponse> appLogic) {
    this.requestParser = requestParser;
    this.appLogic = appLogic;
  }

  @Override
  public void handle(ChannelHandlerContext ctx, Request request, RouteState route) {
    ByteBuf actualBuffer;
    if (request instanceof SegmentedRequestData) {
      SegmentedRequestData segmentedRequest = (SegmentedRequestData)request;
      actualBuffer = segmentedRequest.content();
    } else {
      actualBuffer = request.body();
    }

    HashMap<Integer, GrpcState> session = lazyCreateSession(ctx);
    GrpcState state = session.get(request.streamId());

    if (actualBuffer == null || !actualBuffer.isReadable()) {
      return;
    }

    if (state == null) {
      int size = actualBuffer.slice(1, 4).readInt();

      if (size >= sizeTooLargeToHandle) {
        System.out.println("total data is too big");
        // TODO: return appropriate error and end connection.
        return;
      }

      int packetContentSize = actualBuffer.readableBytes() - 5;
      ByteBuf contentBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(packetContentSize, size);
      contentBuffer.writeBytes(actualBuffer.slice(5, packetContentSize));

      state = new GrpcState(size, contentBuffer);
    } else {
      state.buffer.writeBytes(actualBuffer);
    }

    session.put(request.streamId(), state);

    if (request.endOfMessage()) {
      if (state.size != state.buffer.readableBytes()) {
        // TODO: return appropriate error and end connection.
        System.out.println("the expected length and the actual length do not match");
        return;
      }

      try {
        ByteBuf responseBuffer = makeResponseBuffer(state.buffer.nioBuffer());
        ctx.writeAndFlush(responseBuffer);

      } catch (InvalidProtocolBufferException e) {
        // TODO: return appropriate error and end connection.
        System.out.println("not able to create request object from request bytes");
        return;
      }
    } else {
      // save the state until we have the end of message
      session.put(request.streamId(), state);
    }
  }

  private ByteBuf makeResponseBuffer(ByteBuffer requestBuffer) throws InvalidProtocolBufferException {
    GrpcRequest grpcRequest = requestParser.parse(requestBuffer);
    GrpcResponse grpcResponse = appLogic.apply(grpcRequest);

    byte[] dataBytes = grpcResponse.toByteArray();
    int length = dataBytes.length;
    byte[] lengthByteBuffer = ByteBuffer.allocate(4).putInt(length).array();
    byte[] compressedByteBuffer = ByteBuffer.allocate(1).put((byte)0).array();

    ByteBuf responseBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(length + 5, length + 5);

    responseBuffer.writeBytes(compressedByteBuffer);
    responseBuffer.writeBytes(lengthByteBuffer);
    responseBuffer.writeBytes(dataBytes);

    return responseBuffer;
  }
}
