package com.xjeffrose.xio.http;

import brave.Span;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import javax.annotation.Nonnull;
import lombok.ToString;

@UnstableApi
@ToString
public class StreamingRequestData implements Request, StreamingData {

  private final Request request;
  private final StreamingData data;
  private final Span span;

  public StreamingRequestData(Request request, StreamingData data, Span span) {
    this.request = request;
    this.data = data;
    this.span = span;
  }

  public StreamingRequestData(Request request, StreamingData data) {
    this(request, data, null);
  }

  // region Request

  @Override
  public boolean startOfStream() {
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

  @Nonnull
  @Override
  public Span traceSpan() {
    return span;
  }

  // endregion

  // region StreamingData

  @Override
  public ByteBuf content() {
    return data.content();
  }

  @Override
  public boolean endOfStream() {
    return data.endOfStream();
  }

  @Override
  public Headers trailingHeaders() {
    return data.trailingHeaders();
  }

  // endregion

}
