package com.xjeffrose.xio.http;

import io.netty.handler.codec.http.HttpRequest;

public interface HttpRouter {

  RouteProvider getRouteProvider(HttpRequest request);
}
