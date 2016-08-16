package com.xjeffrose.xio.server.trailhead;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;

interface RouteUpdateProvider {

  void update(HttpContent content);

  void update(LastHttpContent last);

}
