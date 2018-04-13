package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.Message;
import com.xjeffrose.xio.http.SegmentedResponse;
import com.xjeffrose.xio.http.TraceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import javax.annotation.Nullable;
import lombok.ToString;

/** Wrap an incoming HttpResponse, for use in a client. */
@ToString
public class SegmentedHttp1Response implements SegmentedResponse {

  private final HttpResponse delegate;
  private final Headers headers;
  private final TraceInfo traceInfo;
  private final int streamId;

  public SegmentedHttp1Response(HttpResponse delegate, TraceInfo traceInfo, int streamId) {
    this.delegate = delegate;
    this.headers = new Http1Headers(delegate.headers());
    this.traceInfo = traceInfo == null ? new TraceInfo(headers) : traceInfo;
    this.streamId = streamId;
  }

  public SegmentedHttp1Response(HttpResponse delegate, @Nullable Integer streamId) {
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
