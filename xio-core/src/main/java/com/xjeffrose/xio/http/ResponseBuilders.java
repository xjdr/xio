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
        .streamId(request.streamId())
        .status(HttpResponseStatus.NOT_FOUND)
        .build();
  }

  public static Response newServiceUnavailable(Request request) {
    return defaultHeaders(DefaultFullResponse.builder())
        .body(Unpooled.EMPTY_BUFFER)
        .httpTraceInfo(request.httpTraceInfo())
        .streamId(request.streamId())
        .status(HttpResponseStatus.SERVICE_UNAVAILABLE)
        .build();
  }

  public static Response newServiceTimeout(Request request) {
    return defaultHeaders(DefaultFullResponse.builder())
        .body(Unpooled.EMPTY_BUFFER)
        .httpTraceInfo(request.httpTraceInfo())
        .streamId(request.streamId())
        .status(HttpResponseStatus.GATEWAY_TIMEOUT)
        .build();
  }

  public static DefaultFullResponse.Builder newOk() {
    return defaultHeaders(DefaultFullResponse.builder().status(HttpResponseStatus.OK));
  }

  public static DefaultFullResponse.Builder newOk(Request request) {
    return defaultHeaders(
        DefaultFullResponse.builder()
            .streamId(request.streamId())
            .httpTraceInfo(request.httpTraceInfo())
            .status(HttpResponseStatus.OK));
  }
}
