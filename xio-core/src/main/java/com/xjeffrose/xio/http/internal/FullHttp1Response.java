package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.FullResponse;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.TraceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/** Wrap an incoming FullHttpResponse, for use in a client. */
public class FullHttp1Response implements FullResponse {

  private final FullHttpResponse delegate;
  private final Headers headers;
  private final TraceInfo traceInfo;

  public FullHttp1Response(FullHttpResponse delegate, TraceInfo traceInfo) {
    this.delegate = delegate;
    this.headers = new Http1Headers(delegate.headers());
    this.traceInfo = traceInfo == null ? new TraceInfo(headers) : traceInfo;
  }

  public FullHttp1Response(FullHttpResponse delegate) {
    this(delegate, null);
  }

  // region Response

  @Override
  public boolean endOfStream() {
    return true;
  }

  public HttpResponseStatus status() {
    return delegate.status();
  }

  public String version() {
    return delegate.protocolVersion().text();
  }

  public Headers headers() {
    return headers;
  }

  public boolean hasBody() {
    return delegate.content() != null && delegate.content().readableBytes() > 0;
  }

  public ByteBuf body() {
    return delegate.content();
  }

  // endregion

  // region Traceable

  @Override
  public TraceInfo httpTraceInfo() {
    return traceInfo;
  }

  // endregion
}
