package com.xjeffrose.xio.backend.server;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public final class RestHttp2Handler extends Http2ConnectionHandler implements Http2FrameListener {

  private final ImmutableMap<String, RequestHandler> handlers;

  RestHttp2Handler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings,
                   ImmutableMap<String, RequestHandler> handlers) {
    super(decoder, encoder, initialSettings);
    this.handlers = handlers;
  }

  private static Http2Headers http1HeadersToHttp2Headers(FullHttpRequest request) {
    CharSequence host = request.headers().get(HttpHeaderNames.HOST);
    Http2Headers http2Headers = new DefaultHttp2Headers()
      .method(HttpMethod.GET.asciiName())
      .path(request.uri())
      .scheme(HttpScheme.HTTP.name());
    if (host != null) {
      http2Headers.authority(host);
    }
    return http2Headers;
  }

  /**
   * Handles the cleartext HTTP upgrade event. If an upgrade occurred, sends a simple response via HTTP/2
   * on stream 1 (the stream specifically reserved for cleartext HTTP upgrade).
   */
  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent) {
      HttpServerUpgradeHandler.UpgradeEvent upgradeEvent =
        (HttpServerUpgradeHandler.UpgradeEvent) evt;
      onHeadersRead(ctx, 1, http1HeadersToHttp2Headers(upgradeEvent.upgradeRequest()), 0, true);
    }
    super.userEventTriggered(ctx, evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
    cause.printStackTrace();
    ctx.close();
  }

  private void sendResponse(ChannelHandlerContext ctx, int streamId, ResponseBuilder responseBuilder) {
    // Send a frame for the response status
    Http2Headers headers = responseBuilder.buildH2Headers();
    encoder().writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
    encoder().writeData(ctx, streamId, responseBuilder.buildH2BodyData(), 0, true, ctx.newPromise());

    // no need to call flush as channelReadComplete(...) will take care of it.
  }

  @Override
  public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) {
    int processed = data.readableBytes() + padding;
    if (endOfStream) {
      ResponseBuilder responseBuilder = new ResponseBuilder(ctx.alloc());
      responseBuilder.setBodyData(data.retain()).setStatus(OK);
      sendResponse(ctx, streamId, responseBuilder);
    }
    return processed;
  }

  @Override
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                            Http2Headers headers, int padding, boolean endOfStream) {
    if (endOfStream) {
      RequestHandler handler = handlers.get(headers.path().toString());
      ResponseBuilder responseBuilder = new ResponseBuilder(ctx.alloc());

      try {
        if (handler != null) {
          responseBuilder = handler.request(responseBuilder);
        } else {
          responseBuilder = new ResponseBuilder(ctx.alloc())
            .setStatus(HttpResponseStatus.NOT_FOUND)
            .addHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setBody(new PojoResponse("ruh roh", "not found!"));
        }
      } catch (Exception e) {
        responseBuilder.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
          .setBodyData(Unpooled.EMPTY_BUFFER);
      }

      sendResponse(ctx, streamId, responseBuilder);
    }
  }

  @Override
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
                            short weight, boolean exclusive, int padding, boolean endOfStream) {
    onHeadersRead(ctx, streamId, headers, padding, endOfStream);
  }

  @Override
  public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency,
                             short weight, boolean exclusive) {
  }

  @Override
  public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
  }

  @Override
  public void onSettingsAckRead(ChannelHandlerContext ctx) {
  }

  @Override
  public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) {
  }

  @Override
  public void onPingRead(ChannelHandlerContext ctx, ByteBuf data) {

  }

  @Override
  public void onPingAckRead(ChannelHandlerContext ctx, ByteBuf data) {

  }

  @Override
  public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId,
                                Http2Headers headers, int padding) {
  }

  @Override
  public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {
  }

  @Override
  public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) {
  }

  @Override
  public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId,
                             Http2Flags flags, ByteBuf payload) {
  }
}
