package com.xjeffrose.xio.tracing;

import io.netty.channel.CombinedChannelDuplexHandler;

public class HttpClientTracingHandler
    extends CombinedChannelDuplexHandler<
        HttpClientResponseTracingHandler, HttpClientRequestTracingHandler> {

  private static HttpClientResponseTracingHandler buildInbound(HttpClientTracingDispatch state) {
    return new HttpClientResponseTracingHandler(state);
  }

  private static HttpClientRequestTracingHandler buildOutbound(HttpClientTracingDispatch state) {
    return new HttpClientRequestTracingHandler(state);
  }

  public HttpClientTracingHandler(HttpClientTracingDispatch state) {
    super(buildInbound(state), buildOutbound(state));
  }
}
