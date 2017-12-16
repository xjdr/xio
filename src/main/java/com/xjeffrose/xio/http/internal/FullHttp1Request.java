package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.FullRequest;
import com.xjeffrose.xio.http.Headers;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;

public class FullHttp1Request implements FullRequest {

  private final FullHttpRequest delegate;
  private final Http1Headers headers;

  public FullHttp1Request(FullHttpRequest delegate) {
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

  public boolean keepAlive() {
    return HttpUtil.isKeepAlive(delegate);
  }

  public boolean hasBody() {
    return delegate.content() != null && delegate.content().readableBytes() > 0;
  }

  public ByteBuf body() {
    return delegate.content();
  }
}
