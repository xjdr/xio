package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.ToString;

@UnstableApi
@ToString
public class StreamingResponseData implements Response, StreamingData {

  private final Response response;
  private final StreamingData data;

  public StreamingResponseData(Response response, StreamingData data) {
    this.response = response;
    this.data = data;
  }

  public HttpResponseStatus status() {
    return response.status();
  }

  public String version() {
    return response.version();
  }

  public Headers headers() {
    return response.headers();
  }

  public boolean hasBody() {
    return response.hasBody();
  }

  public ByteBuf body() {
    return response.body();
  }

  public ByteBuf content() {
    return data.content();
  }

  public boolean endOfStream() {
    return data.endOfStream();
  }

  public Headers trailingHeaders() {
    return data.trailingHeaders();
  }
}
