package com.xjeffrose.xio.backend.server;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public final class RestHttp2Handler extends Http2ConnectionHandler implements Http2FrameListener {

  private static final AttributeKey<Map<Integer, Http2Headers>> HEADER_KEY = AttributeKey.newInstance("header_key");

  public static Map<Integer, Http2Headers> headersMap(ChannelHandlerContext ctx) {
    Map<Integer, Http2Headers> map = ctx.channel().attr(HEADER_KEY).get();
    if (map == null) {
      map = new HashMap<>();
      ctx.channel().attr(HEADER_KEY).set(map);
    }
    return map;
  }

  public static void setHeaders(ChannelHandlerContext ctx, Integer streamId, Http2Headers headers) {
    Http2Headers saveHeaders = getHeaders(ctx, streamId)
      .map(prevHeaders -> prevHeaders.add(headers))
      .orElse(headers);

    headersMap(ctx).put(streamId, saveHeaders);
  }

  public static Optional<Http2Headers> getHeaders(ChannelHandlerContext ctx, Integer streamId) {
    Map<Integer, Http2Headers> map = headersMap(ctx);
    return Optional.ofNullable(map.get(streamId));
  }

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
    log.error("exception {}", cause);
    ctx.close();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    log.debug("channelReadComplete");
    super.channelReadComplete(ctx);
  }

  private ResponseBuilder errorResponseBuilder(ChannelHandlerContext ctx) {
    return new ResponseBuilder(ctx.alloc())
      .setBodyData(Unpooled.EMPTY_BUFFER)
      .setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
  }

  private Optional<RequestHandler> handler(Http2Headers headers) {
    return Optional.ofNullable(headers.path())
      .map(path -> {
        log.debug("path: {}", path);
        return handlers.get(path.toString());
      });
  }

  private void sendResponse(ChannelHandlerContext ctx, int streamId) {

    ResponseBuilder responseBuilder = getHeaders(ctx, streamId)
      .flatMap(headers -> handler(headers)
        .map(handler -> {
          log.debug("found handler");
          ResponseBuilder builder = new ResponseBuilder(ctx.alloc())
            .addEcho(headers.get("x-echo", "none"))
            .addMethodEcho(headers.method());
          try {
            return handler
              .request(builder);
          } catch (Exception e) {
            log.error("error", e);
            return errorResponseBuilder(ctx);
          }
        })).orElseGet(() -> errorResponseBuilder(ctx));

    // Send a frame for the response status
    Http2Headers headers = responseBuilder.buildH2Headers();
    log.debug("sendResponse headers: {}", headers);
    encoder().writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
    encoder().writeData(ctx, streamId, responseBuilder.buildBodyData(), 0, true, ctx.newPromise());

    // no need to call flush as channelReadComplete(...) will take care of it.
  }

  @Override
  public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) {
    int processed = data.readableBytes() + padding;
    log.debug("onDataRead eos:{} data:{}", endOfStream, data);
    if (endOfStream) {
      sendResponse(ctx, streamId);
    }
    return processed;
  }

  @Override
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId,
                            Http2Headers headers, int padding, boolean endOfStream) {
    log.debug("onHeadersRead eos:{} headers:{}", endOfStream, headers);
    setHeaders(ctx, streamId, headers);
    if (endOfStream) {
      sendResponse(ctx, streamId);
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
