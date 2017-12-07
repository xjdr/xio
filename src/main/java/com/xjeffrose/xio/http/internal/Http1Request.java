package com.xjeffrose.xio.http.internal;

import io.netty.handler.codec.http.HttpMethod;
import com.xjeffrose.xio.core.internal.UnstableApi;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.Request;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Wrap an incoming HttpResponse, for use in a server.
 */
public class Http1Request extends Request {

  protected final HttpRequest delegate;
  private final Http1Headers headers;

  public Http1Request(HttpRequest delegate) {
    this.delegate = delegate;
    headers = new Http1Headers(delegate.headers());
  }

  public HttpMethod method() {
    return delegate.method();
  }
  public String path() {
    return delegate.uri();
  }
  public String version() {
    return delegate.protocolVersion().text();
  }
  public Headers headers() {
    return headers;
  }

}
