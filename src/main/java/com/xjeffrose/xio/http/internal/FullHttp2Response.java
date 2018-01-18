package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.FullResponse;
import com.xjeffrose.xio.http.Headers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.Http2Exception;

/** Wrap an incoming Http2 Response, for use in a client. */
public class FullHttp2Response implements FullResponse {

  private final Http2Headers delegate;
  private final Http2HeadersWrapper headers;
  private final int streamId;

  public FullHttp2Response(Http2Headers delegate, int streamId) {
    this.delegate = delegate;
    headers = new Http2HeadersWrapper(delegate);
    this.streamId = streamId;
  }

  /**
   * Throws a RuntimeException if the underlying status cannot be converted to an HttpResponseStatus
   */
  @Override
  public HttpResponseStatus status() {
    try {
      return HttpConversionUtil.parseStatus(delegate.status());
    } catch (Http2Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int streamId() {
    return streamId;
  }

  @Override
  public String version() {
    return "h2";
  }

  @Override
  public Headers headers() {
    return headers;
  }

  @Override
  public boolean hasBody() {
    return false;
  }

  @Override
  public ByteBuf body() {
    return Unpooled.EMPTY_BUFFER;
  }
}
