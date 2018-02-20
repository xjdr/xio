package com.xjeffrose.xio.tracing;

import io.netty.channel.CombinedChannelDuplexHandler;

public class HttpClientTracingHandler
    extends CombinedChannelDuplexHandler<
        HttpClientResponseTracingHandler, HttpClientRequestTracingHandler> {

  private static HttpClientResponseTracingHandler buildInbound(HttpClientTracingState state) {
    return new HttpClientResponseTracingHandler(state);
  }

  private static HttpClientRequestTracingHandler buildOutbound(HttpClientTracingState state) {
    return new HttpClientRequestTracingHandler(state);
  }

  public HttpClientTracingHandler(HttpClientTracingState state) {
    super(buildInbound(state), buildOutbound(state));
  }
}
