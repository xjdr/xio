package com.xjeffrose.xio.tracing;

import io.netty.channel.CombinedChannelDuplexHandler;

public class HttpServerTracingHandler extends CombinedChannelDuplexHandler<HttpServerRequestTracingHandler, HttpServerResponseTracingHandler> {

  private static HttpServerRequestTracingHandler buildInbound(HttpServerTracingState state) {
    return new HttpServerRequestTracingHandler(state);
  }

  private static HttpServerResponseTracingHandler buildOutbound(HttpServerTracingState state) {
    return new HttpServerResponseTracingHandler(state);
  }

  public HttpServerTracingHandler(HttpServerTracingState state) {
    super(buildInbound(state), buildOutbound(state));
  }
}
