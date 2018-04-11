package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;

/** Interface representing a HTTP1/2 Request */
@UnstableApi
public interface Request extends Message, Traceable {

  HttpMethod method();

  String path();

  String version();

  Headers headers();

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

  /**
   * See: <a href=https://tools.ietf.org/html/rfc7540#section-5>rfc</a>
   *
   * @return the stream id of the http2 connection stream or -1 if http1
   */
  int streamId();

  default boolean hasBody() {
    return false;
  }

  default ByteBuf body() {
    return Unpooled.EMPTY_BUFFER;
  }

  boolean keepAlive();
}
