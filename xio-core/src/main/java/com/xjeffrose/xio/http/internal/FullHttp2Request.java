package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.FullRequest;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.TraceInfo;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2Headers;

public class FullHttp2Request implements FullRequest {

  private final Http2Headers delegate;
  private final Http2HeadersWrapper headers;
  private final int streamId;
  private final TraceInfo traceInfo;

  public FullHttp2Request(Http2Headers delegate, int streamId, TraceInfo traceInfo) {
    this.delegate = delegate;
    this.headers = new Http2HeadersWrapper(delegate);
    this.streamId = streamId;
    this.traceInfo = traceInfo == null ? new TraceInfo(headers) : traceInfo;
  }

  public FullHttp2Request(Http2Headers delegate, int streamId) {
    this(delegate, streamId, null);
  }

  // region Request

  @Override
  public boolean startOfStream() {
    return true;
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

  // endregion

  // region Traceable

  @Override
  public TraceInfo httpTraceInfo() {
    return traceInfo;
  }

  // endregion

}
