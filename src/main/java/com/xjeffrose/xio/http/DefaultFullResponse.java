package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

// TODO(CK): Consolidate Full/Streaming Response Builder into a single builder

/** Value class for representing an outgoing HTTP1/2 Response, for use in a server. */
@UnstableApi
@Builder(builderClassName = "Builder")
@Accessors(fluent = true)
@Getter
@ToString
public class DefaultFullResponse implements FullResponse {

  ByteBuf body;
  HttpResponseStatus status;
  Headers headers;
  /** Not intended to be called. */
  @Override
  public String version() {
    return "";
  }
}
