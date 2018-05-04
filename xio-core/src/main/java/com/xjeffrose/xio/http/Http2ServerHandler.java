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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2ServerHandler extends Http2ConnectionHandler {

  public Http2ServerHandler(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {
    super(decoder, encoder, initialSettings);
  }

  private void writeHeaders(
      ChannelHandlerContext ctx, Http2Headers headers, boolean eos, ChannelPromise promise, int currentStreamId)
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

  private void writeData(ChannelHandlerContext ctx, Http2DataFrame data, ChannelPromise promise, int currentStreamId)
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

    if (msg instanceof Http2Response) {
      Http2Response response = (Http2Response) msg;

      if (response.payload instanceof Http2Headers) {
        writeHeaders(ctx, (Http2Headers) response.payload, response.eos, promise, response.streamId);
        return;
      }

      if (response.payload instanceof Http2DataFrame) {
        writeData(ctx, (Http2DataFrame) response.payload, promise, response.streamId);
        return;
      }
    }
  }
}
