package com.xjeffrose.xio.http;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ProxyResponse implements Response {

  private final Response delegate;
  private final int streamId;

  public ProxyResponse(Response delegate, int streamId) {
    this.delegate = delegate;
    this.streamId = streamId;
  }

  // region Message

  @Override
  public boolean startOfMessage() {
    return delegate.startOfMessage();
  }

  @Override
  public boolean endOfMessage() {
    return delegate.endOfMessage();
  }

  @Override
  public String version() {
    return delegate.version();
  }

  @Override
  public Headers headers() {
    return delegate.headers();
  }

  @Override
  public int streamId() {
    return streamId;
  }

  @Override
  public TraceInfo httpTraceInfo() {
    return delegate.httpTraceInfo();
  }

  @Override
  public boolean hasBody() {
    return delegate.hasBody();
  }

  @Override
  public ByteBuf body() {
    return delegate.body();
  }

  // endregion

  // region Response

  @Override
  public HttpResponseStatus status() {
    return delegate.status();
  }

  // endregion

}
