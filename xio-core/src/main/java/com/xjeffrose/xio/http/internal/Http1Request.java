package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.StreamingRequest;
import com.xjeffrose.xio.http.TraceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import lombok.ToString;

// TODO(CK): Rename this to StreamingHttp1Request

/** Wrap an incoming HttpResponse, for use in a server. */
@ToString
public class Http1Request implements StreamingRequest {

  protected final HttpRequest delegate;
  private final Http1Headers headers;
  private final TraceInfo traceInfo;

  public Http1Request(HttpRequest delegate, TraceInfo traceInfo) {
    this.delegate = delegate;
    this.headers = new Http1Headers(delegate.headers());
    this.traceInfo = traceInfo == null ? new TraceInfo(headers) : traceInfo;
  }

  public Http1Request(HttpRequest delegate) {
    this(delegate, null);
  }

  // region Request

  @Override
  public boolean startOfStream() {
    return true;
  }

  @Override
  public HttpMethod method() {
    return delegate.method();
  }

  @Override
  public String path() {
    return delegate.uri();
  }

  @Override
  public String version() {
    return delegate.protocolVersion().text();
  }

  @Override
  public Headers headers() {
    return headers;
  }

  @Override
  public int streamId() {
    return -1;
  }

  @Override
  public boolean keepAlive() {
    return HttpUtil.isKeepAlive(delegate);
  }

  @Override
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
