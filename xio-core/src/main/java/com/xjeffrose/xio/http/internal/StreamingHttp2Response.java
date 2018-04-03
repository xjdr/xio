package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.StreamingResponse;
import com.xjeffrose.xio.http.TraceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import lombok.ToString;

/** Wrap an incoming Http2 Response, for use in a client. */
@ToString
public class StreamingHttp2Response implements StreamingResponse {

  private final Http2Headers delegate;
  private final Http2HeadersWrapper headers;
  private final int streamId;
  private final TraceInfo traceInfo;

  public StreamingHttp2Response(Http2Headers delegate, int streamId, TraceInfo traceInfo) {
    this.delegate = delegate;
    this.headers = new Http2HeadersWrapper(delegate);
    this.streamId = streamId;
    this.traceInfo = traceInfo == null ? new TraceInfo(headers) : traceInfo;
  }

  public StreamingHttp2Response(Http2Headers delegate, int streamId) {
    this(delegate, streamId, null);
  }

  // region Response

  @Override
  public boolean endOfStream() {
    return false;
  }

  public HttpResponseStatus status() {
    try {
      return HttpConversionUtil.parseStatus(delegate.status());
    } catch (Http2Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String version() {
    return "h2";
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
