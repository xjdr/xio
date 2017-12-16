package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import lombok.experimental.Accessors;
import lombok.Builder;
import lombok.Getter;
import io.netty.handler.codec.http.HttpMethod;

/**
 * Value class for representing a streaming outgoing HTTP1/2 Request, for use in a client.
 */
@UnstableApi
@Builder(builderClassName = "Builder")
@Accessors(fluent = true)
@Getter
public class DefaultStreamingRequest implements StreamingRequest {

  HttpMethod method;
  String path;
  Headers headers;

  /**
   * Not intended to be called.
   */
  @Override
  public String version() { return ""; }

  @Override
  public boolean keepAlive() { return false; }

}
