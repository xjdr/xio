package com.xjeffrose.xio.http.internal;

import io.netty.handler.codec.http.HttpMethod;
import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;

public class FullHttp1Request extends Http1Request {

  public FullHttp1Request(FullHttpRequest delegate) {
    super(delegate);
  }

  FullHttpRequest request() {
    return (FullHttpRequest)delegate;
  }

  public boolean hasBody() {
    return request().content() != null && request().content().readableBytes() > 0;
  }

  public ByteBuf body() {
    return request().content();
  }

}
