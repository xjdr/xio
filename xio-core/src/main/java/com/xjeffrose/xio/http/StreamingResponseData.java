package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.ToString;

/** Interface representing a segmented HTTP1/2 response */
@UnstableApi
@ToString
public class StreamingResponseData implements Response, StreamingData {

  private final Response response;
  private final StreamingData data;
  private final TraceInfo traceInfo;

  public StreamingResponseData(Response response, StreamingData data, TraceInfo traceInfo) {
    this.response = response;
    this.data = data;
    this.traceInfo = traceInfo == null ? new TraceInfo(response.headers()) : traceInfo;
  }

  public StreamingResponseData(Response response, StreamingData data) {
    this(response, data, null);
  }

  // region Response

  @Override
  public HttpResponseStatus status() {
    return response.status();
  }

  @Override
  public String version() {
    return response.version();
  }

  @Override
  public Headers headers() {
    return response.headers();
  }

  @Override
  public boolean hasBody() {
    return response.hasBody();
  }

  @Override
  public ByteBuf body() {
    return response.body();
  }

  // endregion

  // region StreamingData

  @Override
  public ByteBuf content() {
    return data.content();
  }

  @Override
  public boolean startOfMessage() {
    return false;
  }

  @Override
  public boolean endOfMessage() {
    return data.endOfMessage();
  }

  @Override
  public Headers trailingHeaders() {
    return data.trailingHeaders();
  }

  // endregion

  // region Traceable

  @Override
  public TraceInfo httpTraceInfo() {
    return traceInfo;
  }

  // endregion
}
