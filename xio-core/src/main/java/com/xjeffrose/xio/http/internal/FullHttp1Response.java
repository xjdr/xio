package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.FullResponse;
import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.Message;
import com.xjeffrose.xio.http.TraceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import javax.annotation.Nullable;

/** Wrap an incoming FullHttpResponse, for use in a client. */
public class FullHttp1Response implements FullResponse {

  private final FullHttpResponse delegate;
  private final Headers headers;
  private final TraceInfo traceInfo;
  private final int streamId;

  public FullHttp1Response(FullHttpResponse delegate, TraceInfo traceInfo, int streamId) {
    this.delegate = delegate;
    this.headers = new Http1Headers(delegate.headers());
    this.traceInfo = traceInfo == null ? new TraceInfo(headers) : traceInfo;
    this.streamId = streamId;
  }

  public FullHttp1Response(FullHttpResponse delegate, @Nullable Integer streamId) {
    this(delegate, null, streamId == null ? Message.H1_STREAM_ID_NONE : streamId);
  }

  // region Response

  @Override
  public int streamId() {
    return streamId;
  }

  @Override
  public boolean startOfMessage() {
    return true;
  }

  @Override
  public boolean endOfMessage() {
    return true;
  }

  @Override
  public HttpResponseStatus status() {
    return delegate.status();
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
  public boolean hasBody() {
    return delegate.content() != null && delegate.content().readableBytes() > 0;
  }

  @Override
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
