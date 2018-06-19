package com.xjeffrose.xio.http;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.function.Function;

// Documentation for gRPC over HTTP2: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md

public class GrpcRequestHandler<GrpcRequest extends
  com.google.protobuf.GeneratedMessageV3, GrpcResponse extends com.google.protobuf.GeneratedMessageV3> implements PipelineRequestHandler {

  private static final String GRPC_TRAILING_HEADER_STATUS_KEY = "grpc-status";
  private static final String GRPC_CONTENT_TYPE_VALUE = "application/grpc+proto";

  private enum GrpcStatus { // These are defined by gRPC
    OK("0"),
    CANCELLED("1"),
    UNKNOWN("2"),
    INVALID_ARGUMENT("3"),
    DEADLINE_EXCEEDED("4"),
    NOT_FOUND("5"),
    ALREADY_EXISTS("6"),
    PERMISSION_DENIED("7"),
    RESOURCE_EXHAUSTED("8"),
    FAILED_PRECONDITION("9"),
    ABORTED("10"),
    OUT_OF_RANGE("11"),
    UNIMPLEMENTED("12"),
    INTERNAL("13"),
    UNAVAILABLE("14"),
    DATA_LOSS("15"),
    UNAUTHENTICATED("16");

    private final String text;

    GrpcStatus(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }
  }

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

  private static HashMap<Integer, GrpcState> lazyCreateSession(ChannelHandlerContext ctx) {
    HashMap<Integer, GrpcState> session = ctx.channel().attr(CHANNEL_MESSAGE_SESSION_KEY).get();
    if (session == null) {
      session = new HashMap<>();
      ctx.channel().attr(CHANNEL_MESSAGE_SESSION_KEY).set(session);
    }
    return session;
  }

  private final GrpcRequestParser<GrpcRequest> requestParser;
  private final Function<GrpcRequest, GrpcResponse> appLogic;

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
      boolean haveMetaData = actualBuffer.isReadable(5);
      if (!haveMetaData) {
        // TODO: Chris, not sure how we want to handle scenarios where we don't get the entire metadata in the first packet
        sendResponse(ctx, request.streamId(), Unpooled.EMPTY_BUFFER, GrpcStatus.INTERNAL);
        return;
      }

      boolean isCompressed = actualBuffer.slice(0, 1).readBoolean();
      if (isCompressed) {
        sendResponse(ctx, request.streamId(), Unpooled.EMPTY_BUFFER, GrpcStatus.UNIMPLEMENTED);
        return;
      }

      int size = actualBuffer.slice(1, 4).readInt();
      if (size >= sizeTooLargeToHandle) {
        // TODO: Chris, how do we shut down this request so we don't recieve future packets
        sendResponse(ctx, request.streamId(), Unpooled.EMPTY_BUFFER, GrpcStatus.RESOURCE_EXHAUSTED);
        return;
      }

      int packetContentSize = actualBuffer.readableBytes() - 5;

      boolean firstChunkIsLargerThanIndicatedSize = packetContentSize > size;
      if (firstChunkIsLargerThanIndicatedSize) {
        // TODO: Chris, how do we shut down this request so we don't recieve future packets
        sendResponse(ctx, request.streamId(), Unpooled.EMPTY_BUFFER, GrpcStatus.INTERNAL);
        return;
      }

      ByteBuf contentBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(packetContentSize, size);
      contentBuffer.writeBytes(actualBuffer.slice(5, packetContentSize));

      state = new GrpcState(size, contentBuffer);
    } else {
      boolean accumulatedIsLargerThanIndicatedSize = state.buffer.readableBytes() + actualBuffer.readableBytes() > state.size;
      if (accumulatedIsLargerThanIndicatedSize) {
        // TODO: Chris, how do we shut down this request so we don't recieve future packets
        sendResponse(ctx, request.streamId(), Unpooled.EMPTY_BUFFER, GrpcStatus.INTERNAL);
        return;
      }

      state.buffer.writeBytes(actualBuffer);
    }

    if (request.endOfMessage()) {
      handleGrpcRequest(ctx, state, request.streamId());
    } else {
      session.put(request.streamId(), state);
    }
  }

  private void handleGrpcRequest(ChannelHandlerContext ctx, GrpcState state, int streamId) {
    if (state.size != state.buffer.readableBytes()) {
      // TODO: Chris, how do we shut down this request so we don't recieve future packets
      sendResponse(ctx, streamId, Unpooled.EMPTY_BUFFER, GrpcStatus.INTERNAL);
      return;
    }

    try {
      ByteBuf grpcResponseBuffer = makeResponseBuffer(state.buffer.nioBuffer());
      sendResponse(ctx, streamId, grpcResponseBuffer, GrpcStatus.OK);
    } catch (InvalidProtocolBufferException e) {
      sendResponse(ctx, streamId, Unpooled.EMPTY_BUFFER, GrpcStatus.INTERNAL);
    }
  }

  private void sendResponse(ChannelHandlerContext ctx, int streamId, ByteBuf grpcResponseBuffer, GrpcStatus status) {
    Headers headers = new DefaultHeaders()
      .set(HttpHeaderNames.CONTENT_TYPE, GRPC_CONTENT_TYPE_VALUE);
    DefaultSegmentedResponse segmentedResponse = DefaultSegmentedResponse
      .builder()
      .status(HttpResponseStatus.OK)
      .streamId(streamId)
      .headers(headers)
      .build();

    ctx.writeAndFlush(segmentedResponse);

    // TODO: need to add status-message
    Headers trailingHeaders = new DefaultHeaders()
      .set(GRPC_TRAILING_HEADER_STATUS_KEY, status.toString());
    DefaultSegmentedData data = DefaultSegmentedData
      .builder()
      .content(grpcResponseBuffer)
      .trailingHeaders(trailingHeaders)
      .endOfMessage(true)
      .build();

    ctx.writeAndFlush(data);

    // TODO: Chris, I assume we need to remove the state at this point
    HashMap<Integer, GrpcState> session = lazyCreateSession(ctx);
    session.remove(streamId);
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
