package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.StreamingResponse;
import com.xjeffrose.xio.http.TraceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.ToString;

// TODO(CK): Rename this to StreamingHttp1Response

/** Wrap an incoming HttpResponse, for use in a client. */
@ToString
public class Http1Response implements StreamingResponse {

  private final HttpResponse delegate;
  private final Headers headers;
  private final TraceInfo traceInfo;

  public Http1Response(HttpResponse delegate, TraceInfo traceInfo) {
    this.delegate = delegate;
    this.headers = new Http1Headers(delegate.headers());
    this.traceInfo = traceInfo == null ? new TraceInfo(headers) : traceInfo;
  }

  public Http1Response(HttpResponse delegate) {
    this(delegate, null);
  }

  // region Response

  @Override
  public boolean endOfStream() {
    return false;
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

  public ByteBuf body() {
    return Unpooled.EMPTY_BUFFER;
  }

  // endregion

  // region Traceable

  @Override
  public TraceInfo httpTraceInfo() {
    return traceInfo;
  }

  // endregion
}
