package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

@UnstableApi
public interface Response {

  HttpResponseStatus status();

  String version();

  Headers headers();

  default boolean hasBody() {
    return false;
  }

  default ByteBuf body() {
    return null;
  }
}
