package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import lombok.ToString;

@UnstableApi
@ToString
public class StreamingRequestData implements Request, StreamingData {

  private final Request request;
  private final StreamingData data;

  public StreamingRequestData(Request request, StreamingData data) {
    this.request = request;
    this.data = data;
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
}
