package com.xjeffrose.xio.http.internal;

import com.xjeffrose.xio.http.Headers;
import com.xjeffrose.xio.http.StreamingResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.ToString;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.Http2Exception;

/** Wrap an incoming Http2 Response, for use in a client. */
@ToString
public class StreamingHttp2Response implements StreamingResponse {

  private final Http2Headers delegate;
  private final Http2HeadersWrapper headers;
  private final int streamId;

  public StreamingHttp2Response(Http2Headers delegate, int streamId) {
    this.delegate = delegate;
    headers = new Http2HeadersWrapper(delegate);
    this.streamId = streamId;
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
}
