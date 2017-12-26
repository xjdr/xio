package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Value class for representing a streaming outgoing HTTP1/2 Response, for use in a server. */
@UnstableApi
@Builder(builderClassName = "Builder")
@Accessors(fluent = true)
@Getter
public class DefaultStreamingResponse implements StreamingResponse {

  HttpResponseStatus status;
  Headers headers;
  /** Not intended to be called. */
  @Override
  public String version() {
    return "";
  }
}
