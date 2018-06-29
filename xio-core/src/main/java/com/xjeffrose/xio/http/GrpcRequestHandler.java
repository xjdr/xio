package com.xjeffrose.xio.http;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.function.Function;

// Documentation for gRPC over HTTP2: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md

public class GrpcRequestHandler<
        GrpcRequest extends com.google.protobuf.GeneratedMessageV3,
        GrpcResponse extends com.google.protobuf.GeneratedMessageV3>
    implements PipelineRequestHandler {

  private static final int METADATA_SIZE = 5;
  private static final int MAX_PAYLOAD_SIZE = 1_000_000;
  private static final String GRPC_TRAILING_HEADER_STATUS_KEY = "grpc-status";
  private static final String GRPC_TRAILING_HEADER_MESSAGE_KEY = "grpc-message";
  private static final String GRPC_CONTENT_TYPE_VALUE = "application/grpc+proto";
  private static final String GRPC_MESSAGE_NO_COMPRESSION = "compression not supported";
  private static final String GRPC_MESSAGE_LARGE_SIZE = "payload is too large";
  private static final String GRPC_MESSAGE_NO_METADATA = "metadata not provided";
  private static final String GRPC_MESSAGE_WRONG_SIZE =
      "indicated payload size does not match actual payload size";
  private static final String GRPC_MESSAGE_CANNOT_MAKE_RESPONSE =
      "unable to create response object";

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

  public GrpcRequestHandler(
      GrpcRequestParser<GrpcRequest> requestParser, Function<GrpcRequest, GrpcResponse> appLogic) {
    this.requestParser = requestParser;
    this.appLogic = appLogic;
  }

  public Function<GrpcRequest, GrpcResponse> getAppLogic() {
    return appLogic;
  }

  @Override
  public void handle(ChannelHandlerContext ctx, Request request, RouteState route) {
    ByteBuf actualBuffer;
    if (request instanceof SegmentedRequestData) {
      SegmentedRequestData segmentedRequest = (SegmentedRequestData) request;
      actualBuffer = segmentedRequest.content();
    } else {
      actualBuffer = request.body();
    }

    if (actualBuffer == null || !actualBuffer.isReadable()) {
      return;
    }

    HashMap<Integer, GrpcState> session = lazyCreateSession(ctx);
    GrpcState state = session.get(request.streamId());

    if (state == null) {
      boolean hasMetaData = actualBuffer.isReadable(METADATA_SIZE);
      if (!hasMetaData) {
        sendResponse(
            ctx,
            request.streamId(),
            Unpooled.EMPTY_BUFFER,
            Status.INTERNAL,
            GRPC_MESSAGE_NO_METADATA);
        return;
      }

      boolean isCompressed = actualBuffer.slice(0, 1).readBoolean();
      if (isCompressed) {
        sendResponse(
            ctx,
            request.streamId(),
            Unpooled.EMPTY_BUFFER,
            Status.UNIMPLEMENTED,
            GRPC_MESSAGE_NO_COMPRESSION);
        return;
      }

      int size = actualBuffer.slice(1, 4).readInt();
      if (size >= MAX_PAYLOAD_SIZE) {
        sendResponse(
            ctx,
            request.streamId(),
            Unpooled.EMPTY_BUFFER,
            Status.RESOURCE_EXHAUSTED,
            GRPC_MESSAGE_LARGE_SIZE);
        return;
      }

      int packetContentSize = actualBuffer.readableBytes() - METADATA_SIZE;

      boolean firstChunkIsLargerThanIndicatedSize = packetContentSize > size;
      if (firstChunkIsLargerThanIndicatedSize) {
        sendResponse(
            ctx,
            request.streamId(),
            Unpooled.EMPTY_BUFFER,
            Status.INTERNAL,
            GRPC_MESSAGE_WRONG_SIZE);
        return;
      }

      ByteBuf contentBuffer = UnpooledByteBufAllocator.DEFAULT.buffer(packetContentSize, size);
      contentBuffer.writeBytes(actualBuffer.slice(METADATA_SIZE, packetContentSize));

      state = new GrpcState(size, contentBuffer);
    } else {
      boolean accumulatedIsLargerThanIndicatedSize =
          state.buffer.readableBytes() + actualBuffer.readableBytes() > state.size;
      if (accumulatedIsLargerThanIndicatedSize) {
        sendResponse(
            ctx,
            request.streamId(),
            Unpooled.EMPTY_BUFFER,
            Status.INTERNAL,
            GRPC_MESSAGE_WRONG_SIZE);
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
      sendResponse(
          ctx, streamId, Unpooled.EMPTY_BUFFER, Status.INTERNAL, GRPC_MESSAGE_WRONG_SIZE);
      return;
    }

    try {
      ByteBuf grpcResponseBuffer = makeResponseBuffer(state.buffer.nioBuffer());
      sendResponse(ctx, streamId, grpcResponseBuffer, Status.OK, "");
    } catch (InvalidProtocolBufferException e) {
      sendResponse(
          ctx,
          streamId,
          Unpooled.EMPTY_BUFFER,
          Status.INTERNAL,
          GRPC_MESSAGE_CANNOT_MAKE_RESPONSE);
    }
  }

  private void sendResponse(
      ChannelHandlerContext ctx,
      int streamId,
      ByteBuf grpcResponseBuffer,
      Status status,
      String statusMessage) {
    Headers headers =
        new DefaultHeaders().set(HttpHeaderNames.CONTENT_TYPE, GRPC_CONTENT_TYPE_VALUE);
    DefaultSegmentedResponse segmentedResponse =
        DefaultSegmentedResponse.builder()
            .streamId(streamId)
            .status(HttpResponseStatus.OK)
            .headers(headers)
            .build();

    ctx.writeAndFlush(segmentedResponse);

    Headers trailingHeaders =
        new DefaultHeaders().set(GRPC_TRAILING_HEADER_STATUS_KEY, Integer.toString(status.getCode().value()));

    if (!statusMessage.isEmpty()) {
      trailingHeaders.add(GRPC_TRAILING_HEADER_MESSAGE_KEY, grpcEncodedString(statusMessage));
    }

    DefaultSegmentedData data =
        DefaultSegmentedData.builder()
            .streamId(streamId)
            .content(grpcResponseBuffer)
            .trailingHeaders(trailingHeaders)
            .endOfMessage(true)
            .build();

    ctx.writeAndFlush(data);

    HashMap<Integer, GrpcState> session = lazyCreateSession(ctx);
    session.remove(streamId);

    if (status != Status.OK) {
      ctx.close();
    }
  }

  private ByteBuf makeResponseBuffer(ByteBuffer requestBuffer)
      throws InvalidProtocolBufferException {
    GrpcRequest grpcRequest = requestParser.parse(requestBuffer);
    GrpcResponse grpcResponse = appLogic.apply(grpcRequest);

    byte[] dataBytes = grpcResponse.toByteArray();
    int length = dataBytes.length;
    byte[] lengthByteBuffer = ByteBuffer.allocate(4).putInt(length).array();
    byte[] compressedByteBuffer = ByteBuffer.allocate(1).put((byte) 0).array();

    ByteBuf responseBuffer =
        UnpooledByteBufAllocator.DEFAULT.buffer(length + METADATA_SIZE, length + METADATA_SIZE);

    responseBuffer.writeBytes(compressedByteBuffer);
    responseBuffer.writeBytes(lengthByteBuffer);
    responseBuffer.writeBytes(dataBytes);

    return responseBuffer;
  }

  private String grpcEncodedString(String input) {
    StringBuilder output = new StringBuilder();
    byte[] bytes;

    try {
      bytes = input.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      return "";
    }

    for (byte b : bytes) {
      output.append(String.format("%%%02x", b));
    }
    return output.toString();
  }
}
