package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import lombok.experimental.Accessors;
import lombok.Builder;
import lombok.Getter;

// TODO(CK): consider using auto builders for this:
// https://github.com/google/auto/blob/master/value/userguide/builders-howto.md#validate
// builder().get().path("/foo/bar")

/**
 * Value class for representing an outgoing HTTP1/2 Request, for use in a client.
 */
@UnstableApi
@Builder(builderClassName = "Builder")
@Accessors(fluent = true)
@Getter
public class FullRequest extends Request {

  ByteBuf body;
  HttpMethod method;
  String path;
  Headers headers;
  /**
   * Not intended to be called.
   */
  @Override
  public String version() { return ""; }

}
