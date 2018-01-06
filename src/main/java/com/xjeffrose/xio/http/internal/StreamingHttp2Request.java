package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.StreamingRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.ToString;

/** Wrap an incoming Http2 Request, for use in a server. */
@ToString
public class StreamingHttp2Request implements StreamingRequest {

  private final Http2Headers delegate;
  private final Http2HeadersWrapper headers;
  private final int streamId;

  public StreamingHttp2Request(Http2Headers delegate, int streamId) {
    this.delegate = delegate;
    headers = new Http2HeadersWrapper(delegate);
    this.streamId = streamId;
  }

  @Override
  public HttpMethod method() {
    return HttpMethod.valueOf(delegate.method().toString());
  }

  @Override
  public String host() {
    return delegate.authority().toString();
  }

  @Override
  public int streamId() {
    return streamId;
  }

  @Override
  public String path() {
    return delegate.path().toString();
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
  public boolean keepAlive() {
    return true;
  }

  @Override
  public ByteBuf body() {
    return Unpooled.EMPTY_BUFFER;
  }
}
