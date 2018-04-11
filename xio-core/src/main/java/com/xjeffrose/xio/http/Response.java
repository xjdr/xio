package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;

/** Interface representing a HTTP1/2 Response */
@UnstableApi
public interface Response extends Message {

  HttpResponseStatus status();

  @Override
  default int streamId() {
    return H1_STREAM_ID_NONE;
  }

  @Override
  default boolean hasBody() {
    return false;
  }

  @Override
  default ByteBuf body() {
    return null;
  }
}
