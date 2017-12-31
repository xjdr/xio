package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.FullResponse;
import com.xjeffrose.xio.http.Headers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Headers;

/** Wrap an incoming Http2 Response, for use in a client. */
public class FullHttp2Response implements FullResponse {

  private final Http2Headers delegate;
  private final Http2HeadersWrapper headers;

  public FullHttp2Response(Http2Headers delegate) {
    this.delegate = delegate;
    headers = new Http2HeadersWrapper(delegate);
  }

  public HttpResponseStatus status() {
    try {
      return HttpResponseStatus.valueOf(Integer.parseInt(delegate.status().toString()));
    } catch (Exception e) {
      return null;
    }
  }

  public String version() {
    return "h2";
  }

  public Headers headers() {
    return headers;
  }

  public boolean hasBody() {
    return false;
  }

  public ByteBuf body() {
    return Unpooled.EMPTY_BUFFER;
  }
}
