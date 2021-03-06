package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import javax.annotation.Nonnull;
import lombok.ToString;

@UnstableApi
@ToString
public class SegmentedRequestData implements Request, SegmentedData {

  private final Request request;
  private final SegmentedData data;
  private final TraceInfo traceInfo;

  public SegmentedRequestData(Request request, SegmentedData data, TraceInfo traceInfo) {
    this.request = request;
    this.data = data;
    this.traceInfo = traceInfo == null ? new TraceInfo(request.headers()) : traceInfo;
  }

  public SegmentedRequestData(@Nonnull Request request, SegmentedData data) {
    this(request, data, null);
  }

  // region Request

  @Override
  public String host() {
    return request.host();
  }

  @Override
  public boolean startOfMessage() {
    return false;
  }

  @Override
  public HttpMethod method() {
    return request.method();
  }

  @Override
  public String path() {
    return request.path();
  }

  @Override
  public String version() {
    return request.version();
  }

  @Override
  public Headers headers() {
    return request.headers();
  }

  @Override
  public int streamId() {
    return request.streamId();
  }

  @Override
  public boolean keepAlive() {
    return request.keepAlive();
  }

  @Override
  public boolean hasBody() {
    return request.hasBody();
  }

  @Override
  public ByteBuf body() {
    return request.body();
  }

  // endregion

  // region Traceable

  @Override
  public TraceInfo httpTraceInfo() {
    return traceInfo;
  }

  // endregion

  // region SegmentedData

  @Override
  public ByteBuf content() {
    return data.content();
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

}
