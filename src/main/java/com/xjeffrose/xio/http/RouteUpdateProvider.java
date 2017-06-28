package com.xjeffrose.xio.http;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

interface RouteUpdateProvider {

  void update(HttpContent content);

  void update(LastHttpContent last);

}
