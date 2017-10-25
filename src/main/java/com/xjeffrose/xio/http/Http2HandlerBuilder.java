package com.xjeffrose.xio.http;

import static io.netty.handler.logging.LogLevel.INFO;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http2HandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<Http2Handler, Http2HandlerBuilder> {

  private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, Http2Handler.class);
  private final Function<Boolean, Http2FrameListener> frameListener;

  public Http2HandlerBuilder(Function<Boolean, Http2FrameListener> frameListener) {
    this.frameListener = frameListener;
    frameLogger(logger);
  }

  public Http2HandlerBuilder() {
    this(Http2FrameForwarder::create);
  }

  @Override
  public Http2Handler build() {
    frameListener(this.frameListener.apply(this.isServer()));
    return super.build();
  }

  @Override
  public Http2HandlerBuilder server(boolean isServer) {
    super.server(isServer);
    return this;
  }

  @Override
  protected Http2Handler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings) {
    return new Http2Handler(decoder, encoder, initialSettings);
  }

}
