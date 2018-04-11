package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

/** Interface representing a HTTP1/2 Response */
@UnstableApi
public interface Response extends Message, Traceable {

  HttpResponseStatus status();

  String version();

  Headers headers();

  /**
   * See: <a href=https://tools.ietf.org/html/rfc7540#section-5>rfc</a>
   *
   * @return the stream id of the http2 connection stream or -1 if http1
   */
  default int streamId() {
    return STREAM_ID_NONE;
  }

  default boolean hasBody() {
    return false;
  }

  default ByteBuf body() {
    return null;
  }
}
