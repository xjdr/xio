package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.*;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2ClientHandler extends Http2ConnectionHandler {

  private static final AttributeKey<Integer> STREAM_ID_KEY =
      AttributeKey.newInstance("xio_h2_client_stream_id");

  public static void setCurrentStreamId(ChannelHandlerContext ctx, int streamId) {
    ctx.channel().attr(STREAM_ID_KEY).set(streamId);
  }

  private int currentStreamId;

  public Http2ClientHandler(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {
    super(decoder, encoder, initialSettings);
  }

  private void writeHeaders(
      ChannelHandlerContext ctx, Http2Headers headers, boolean eos, ChannelPromise promise)
      throws Exception {
    encoder()
        .writeHeaders(
            ctx,
            currentStreamId,
            headers,
            0,
            Http2CodecUtil.DEFAULT_PRIORITY_WEIGHT,
            false,
            0,
            eos,
            promise);
  }

  private void writeData(ChannelHandlerContext ctx, Http2DataFrame data, ChannelPromise promise)
      throws Exception {
    encoder().writeData(ctx, currentStreamId, data.content(), 0, data.isEndStream(), promise);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    // not an h2 frame, forward the write
    if (!(msg instanceof Http2DataFrame
        || msg instanceof Http2Headers
        || msg instanceof Http2Request
        || msg instanceof Http2Response)) {
      ctx.write(msg, promise);
      return;
    }

    if (msg instanceof Http2Request) {
      Http2Request request = (Http2Request) msg;

      if (request.payload instanceof Http2Headers) {
        Http2Headers headers = (Http2Headers) request.payload;
        currentStreamId = connection().local().incrementAndGetNextStreamId();
        setCurrentStreamId(ctx, currentStreamId);
        writeHeaders(ctx, headers, request.eos, promise);
        return;
      }

      if (request.payload instanceof Http2DataFrame) {
        Http2DataFrame data = (Http2DataFrame) request.payload;
        writeData(ctx, data, promise);
        return;
      }
    }

    if (msg instanceof Http2Headers) {
      Http2Headers headers = (Http2Headers) msg;
      currentStreamId = connection().local().incrementAndGetNextStreamId();
      setCurrentStreamId(ctx, currentStreamId);
      writeHeaders(ctx, headers, false, promise);
      return;
    }

    if (msg instanceof Http2DataFrame) {
      Http2DataFrame data = (Http2DataFrame) msg;
      writeData(ctx, data, promise);
      return;
    }
  }
}
