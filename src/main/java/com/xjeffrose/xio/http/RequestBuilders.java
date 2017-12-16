package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.internal.UnstableApi;
import io.netty.handler.codec.http.HttpMethod;

@UnstableApi
public class RequestBuilders {

  private static DefaultFullRequest.Builder newDefaultBuilder() {
    return DefaultFullRequest.builder().headers(new DefaultHeaders());
  }

  public static DefaultFullRequest.Builder newGet() {
    return newDefaultBuilder().method(HttpMethod.GET);
  }

  public static DefaultFullRequest.Builder newGet(String path) {
    return newGet().path(path);
  }

  public static DefaultFullRequest.Builder newPost() {
    return newDefaultBuilder().method(HttpMethod.POST);
  }

  public static DefaultFullRequest.Builder newPost(String path) {
    return newPost().path(path);
  }
}
