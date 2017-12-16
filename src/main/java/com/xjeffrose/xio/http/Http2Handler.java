package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2Handler extends Http2ConnectionHandler {

  private static final AttributeKey<Integer> STREAM_ID_KEY =
      AttributeKey.newInstance("xio_h2_stream_id");

  public static int defaultValue(Integer i) {
    if (i == null) {
      return 0;
    }
    return i;
  }

  public static int getCurrentStreamId(ChannelHandlerContext ctx) {
    return defaultValue(ctx.channel().attr(STREAM_ID_KEY).get());
  }

  public static void setCurrentStreamId(ChannelHandlerContext ctx, int streamId) {
    ctx.channel().attr(STREAM_ID_KEY).set(streamId);
  }

  private int currentStreamId;

  public Http2Handler(
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
        || msg instanceof Http2Response)) {
      ctx.write(msg, promise);
      return;
    }

    if (msg instanceof Http2Response) {
      Http2Response response = (Http2Response) msg;

      currentStreamId = response.streamId;
      setCurrentStreamId(ctx, currentStreamId);
      if (response.payload instanceof Http2Headers) {
        writeHeaders(ctx, (Http2Headers) response.payload, response.eos, promise);
        return;
      }

      if (response.payload instanceof Http2DataFrame) {
        writeData(ctx, (Http2DataFrame) response.payload, promise);
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
