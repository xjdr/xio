package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import lombok.experimental.Accessors;
import lombok.Builder;
import lombok.Getter;
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

  public HttpMethod method() {
    return request.method();
  }

  public String path() {
    return request.path();
  }

  public String version() {
    return request.version();
  }

  public Headers headers() {
    return request.headers();
  }

  public boolean keepAlive() {
    return request.keepAlive();
  }

  public boolean hasBody() {
    return request.hasBody();
  }

  public ByteBuf body() {
    return request.body();
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
