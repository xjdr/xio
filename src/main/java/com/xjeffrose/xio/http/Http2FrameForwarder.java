package com.xjeffrose.xio.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;

/** Forwards the frame with stream id to the next handler in the pipeline */
public class Http2FrameForwarder implements Http2FrameListener {

  private final boolean isServer;

  public Http2FrameForwarder(boolean isServer) {
    this.isServer = isServer;
  }

  public static Http2FrameForwarder create(boolean isServer) {
    return new Http2FrameForwarder(isServer);
  }

  @Override
  public int onDataRead(
      ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
      throws Http2Exception {
    if (isServer) {
      ctx.fireChannelRead(
          Http2Request.build(
              streamId,
              new DefaultHttp2DataFrame(data.retain(), endOfStream, padding),
              endOfStream));
    } else {
      ctx.fireChannelRead(
          Http2Response.build(
              streamId,
              new DefaultHttp2DataFrame(data.retain(), endOfStream, padding),
              endOfStream));
    }
    return data.readableBytes() + padding;
  }

  @Override
  public void onHeadersRead(
      ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream)
      throws Http2Exception {
    if (isServer) {
      ctx.fireChannelRead(Http2Request.build(streamId, headers, endStream));
    } else {
      ctx.fireChannelRead(Http2Response.build(streamId, headers, endStream));
    }
  }

  @Override
  public void onHeadersRead(
      ChannelHandlerContext ctx,
      int streamId,
      Http2Headers headers,
      int streamDependency,
      short weight,
      boolean exclusive,
      int padding,
      boolean endStream)
      throws Http2Exception {
    if (isServer) {
      ctx.fireChannelRead(Http2Request.build(streamId, headers, endStream));
    } else {
      ctx.fireChannelRead(Http2Response.build(streamId, headers, endStream));
    }
  }

  @Override
  public void onPriorityRead(
      ChannelHandlerContext ctx,
      int streamId,
      int streamDependency,
      short weight,
      boolean exclusive)
      throws Http2Exception {
    // TODO(CK): We don't currently have a use case for these frames
  }

  @Override
  public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode)
      throws Http2Exception {
    // TODO(CK): We don't currently have a use case for these frames
  }

  @Override
  public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {
    // TODO(CK): We don't currently have a use case for these frames
  }

  @Override
  public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings)
      throws Http2Exception {
    // TODO(CK): We don't currently have a use case for these frames
  }

  @Override
  public void onPingRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {
    // TODO(CK): We don't currently have a use case for these frames
  }

  @Override
  public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data) throws Http2Exception {
    // TODO(CK): We don't currently have a use case for these frames
  }

  @Override
  public void onPushPromiseRead(
      ChannelHandlerContext ctx,
      int streamId,
      int promisedStreamId,
      Http2Headers headers,
      int padding)
      throws Http2Exception {
    // TODO(CK): We don't currently have a use case for these frames
  }

  @Override
  public void onGoAwayRead(
      ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData)
      throws Http2Exception {
    // TODO(CK): We don't currently have a use case for these frames
  }

  @Override
  public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement)
      throws Http2Exception {
    // TODO(CK): We don't currently have a use case for these frames
  }

  @Override
  public void onUnknownFrame(
      ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload) {
    // TODO(CK): We don't currently have a use case for these frames
  }
}
