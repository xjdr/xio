package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.HttpResponseStatus;

@UnstableApi
public class ResponseBuilders {

  // TODO(CK): move this into the builder?
  public static DefaultFullResponse.Builder defaultHeaders(DefaultFullResponse.Builder builder) {
    return builder.headers(new DefaultHeaders());
  }

  public static Response newNotFound() {
    return defaultHeaders(DefaultFullResponse.builder())
        .status(HttpResponseStatus.NOT_FOUND)
        .build();
  }

  public static DefaultFullResponse.Builder newOk() {
    return defaultHeaders(DefaultFullResponse.builder().status(HttpResponseStatus.OK));
  }
}
