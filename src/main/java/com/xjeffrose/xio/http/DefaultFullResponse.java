package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import lombok.Builder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.ToString;


// TODO(CK): Consolidate Full/Streaming Response Builder into a single builder

/**
 * Value class for representing an outgoing HTTP1/2 Response, for use in a server.
 */
@UnstableApi
@Builder(builderClassName = "Builder")
@Accessors(fluent = true)
@Getter
@ToString
public class DefaultFullResponse implements FullResponse {

  ByteBuf body;
  HttpResponseStatus status;
  Headers headers;
  /**
   * Not intended to be called.
   */
  @Override
  public String version() { return ""; }

}
