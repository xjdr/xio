package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;

@UnstableApi
public class ResponseBuilders {

  // TODO(CK): move this into the builder?
  public static DefaultFullResponse.Builder defaultHeaders(DefaultFullResponse.Builder builder) {
    return builder.headers(new DefaultHeaders());
  }

  public static Response newNotFound(Request request) {
    return defaultHeaders(DefaultFullResponse.builder())
        .body(Unpooled.EMPTY_BUFFER)
        .httpTraceInfo(request.httpTraceInfo())
        .status(HttpResponseStatus.NOT_FOUND)
        .build();
  }

  public static DefaultFullResponse.Builder newOk() {
    return defaultHeaders(DefaultFullResponse.builder().status(HttpResponseStatus.OK));
  }

  public static DefaultFullResponse.Builder newOk(Request request) {
    return defaultHeaders(
        DefaultFullResponse.builder()
            .httpTraceInfo(request.httpTraceInfo())
            .status(HttpResponseStatus.OK));
  }
}
