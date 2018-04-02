package com.xjeffrose.xio.tracing;

import io.netty.channel.CombinedChannelDuplexHandler;

public class HttpServerTracingHandler
    extends CombinedChannelDuplexHandler<
        HttpServerRequestTracingHandler, HttpServerResponseTracingHandler> {

  private static HttpServerRequestTracingHandler buildInbound(HttpServerTracingDispatch state) {
    return new HttpServerRequestTracingHandler(state);
  }

  private static HttpServerResponseTracingHandler buildOutbound(HttpServerTracingDispatch state) {
    return new HttpServerResponseTracingHandler(state);
  }

  public HttpServerTracingHandler(HttpServerTracingDispatch state) {
    super(buildInbound(state), buildOutbound(state));
  }
}
