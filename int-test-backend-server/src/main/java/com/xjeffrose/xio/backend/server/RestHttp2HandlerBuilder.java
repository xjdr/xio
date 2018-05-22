package com.xjeffrose.xio.backend.server;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http2.*;

import static io.netty.handler.logging.LogLevel.INFO;

public final class RestHttp2HandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<RestHttp2Handler, RestHttp2HandlerBuilder> {

  private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, RestHttp2Handler.class);
  private final ImmutableMap<String, RequestHandler> handlers;

  RestHttp2HandlerBuilder(ImmutableMap<String, RequestHandler> handlers) {
    frameLogger(logger);
    this.handlers = handlers;
  }

  @Override
  public RestHttp2Handler build() {
    return super.build();
  }

  @Override
  protected RestHttp2Handler build(Http2ConnectionDecoder decoder,
                                   Http2ConnectionEncoder encoder,
                                   Http2Settings initialSettings) {
    RestHttp2Handler handler = new RestHttp2Handler(decoder, encoder, initialSettings, handlers);
    frameListener(handler);
    return handler;
  }
}
