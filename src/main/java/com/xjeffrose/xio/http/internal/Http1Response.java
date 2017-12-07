package com.xjeffrose.xio.http.internal;

import io.netty.handler.codec.http.HttpResponseStatus;
import com.xjeffrose.xio.core.internal.UnstableApi;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.Response;
import io.netty.handler.codec.http.HttpResponse;


/**
 * Wrap an incoming HttpResponse, for use in a client.
 */
public class Http1Response extends Response {

  private final HttpResponse delegate;
  private final Headers headers;

  public Http1Response(HttpResponse delegate) {
    this.delegate = delegate;
    headers = new Http1Headers(delegate.headers());
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

}
