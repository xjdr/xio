package com.xjeffrose.xio.http;

import io.netty.handler.codec.http.HttpRequest;

public interface Router {
  RequestHandler get(HttpRequest request);
}
