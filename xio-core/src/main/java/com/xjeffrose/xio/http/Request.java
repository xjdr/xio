package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;

/** Interface representing a HTTP1/2 Request */
@UnstableApi
public interface Request extends Message {

  HttpMethod method();

  String path();

  default String host() {
    return headers().get(HttpHeaderNames.HOST.toString());
  }

  default String host(String defaultValue) {
    String result = host();
    if (result == null || result.isEmpty()) {
      return defaultValue;
    }
    return result;
  }

  boolean keepAlive();

  @Override
  default boolean hasBody() {
    return false;
  }

  @Override
  default ByteBuf body() {
    return Unpooled.EMPTY_BUFFER;
  }
}
