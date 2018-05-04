package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2ClientHandler extends Http2ConnectionHandler {

  public Http2ClientHandler(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {
    super(decoder, encoder, initialSettings);
  }

  private void writeHeaders(
      ChannelHandlerContext ctx,
      Http2Headers headers,
      boolean eos,
      ChannelPromise promise,
      int currentStreamId)
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

  private void writeData(
      ChannelHandlerContext ctx, Http2DataFrame data, ChannelPromise promise, int currentStreamId)
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
        int streamId =
            Http2ClientStreamMapper.http2ClientStreamMapper(ctx)
                .outboundStreamId(connection(), request.streamId);
        Http2Headers headers = (Http2Headers) request.payload;
        writeHeaders(ctx, headers, request.eos, promise, streamId);
        return;
      }

      if (request.payload instanceof Http2DataFrame) {
        Http2DataFrame data = (Http2DataFrame) request.payload;
        int streamId =
            Http2ClientStreamMapper.http2ClientStreamMapper(ctx)
                .outboundStreamId(connection(), request.streamId);
        writeData(ctx, data, promise, streamId);
        return;
      }
    }

    if (msg instanceof Http2Headers) {
      Http2Headers headers = (Http2Headers) msg;
      int streamId = Http2ClientStreamMapper.http2ClientStreamMapper(ctx).outboundStreamId(connection(), 0);
      writeHeaders(ctx, headers, false, promise, streamId);
      return;
    }

    if (msg instanceof Http2DataFrame) {
      int streamId = Http2ClientStreamMapper.http2ClientStreamMapper(ctx).outboundStreamId(connection(), 0);
      Http2DataFrame data = (Http2DataFrame) msg;
      writeData(ctx, data, promise, streamId);
      return;
    }
  }
}
