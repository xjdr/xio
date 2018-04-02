package com.xjeffrose.xio.http;

import brave.Span;
import com.xjeffrose.xio.http.internal.Http1Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.Map;
import java.util.Optional;

/** Mutable data object storing http tracing information. */
public class TraceInfo {

  private Span span = null;
  private final Headers headers;

  public TraceInfo(Headers headers) {
    this.headers = headers;
  }

  public Optional<Span> getSpan() {
    return Optional.ofNullable(span);
  }

  public void setSpan(Span span) {
    this.span = span;
  }

  public Headers getHeaders() {
    return headers;
  }
}
