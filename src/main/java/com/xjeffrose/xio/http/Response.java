package com.xjeffrose.xio.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import com.xjeffrose.xio.core.internal.UnstableApi;

@UnstableApi
public abstract class Response {

  public abstract HttpResponseStatus status();
  public abstract String version();
  public abstract Headers headers();

}
