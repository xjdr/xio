package com.xjeffrose.xio.http;

import static io.netty.handler.logging.LogLevel.INFO;

import io.netty.handler.codec.http2.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2HandlerBuilder
    extends AbstractHttp2ConnectionHandlerBuilder<Http2ConnectionHandler, Http2HandlerBuilder> {

  private static final Http2FrameLogger logger =
      new Http2FrameLogger(INFO, Http2ServerHandler.class);
  private final Function<Boolean, Http2FrameListener> frameListener;

  public Http2HandlerBuilder(Function<Boolean, Http2FrameListener> frameListener) {
    this.frameListener = frameListener;
    frameLogger(logger);
  }

  public Http2HandlerBuilder() {
    this(Http2FrameForwarder::create);
  }

  @Override
  public Http2ConnectionHandler build() {
    frameListener(this.frameListener.apply(this.isServer()));
    return super.build();
  }

  @Override
  public Http2HandlerBuilder server(boolean isServer) {
    super.server(isServer);
    return this;
  }

  @Override
  protected Http2ConnectionHandler build(
      Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {
    if (isServer()) {
      return new Http2ServerHandler(decoder, encoder, initialSettings);
    } else {
      return new Http2ClientHandler(decoder, encoder, initialSettings);
    }
  }
}
