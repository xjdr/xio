package com.xjeffrose.xio.backend.server;

import com.google.common.collect.ImmutableMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

import java.util.function.Consumer;

/**
 * Negotiates with the browser if HTTP2 or HTTP is going to be used. Once decided, the Netty
 * pipeline is setup with the correct handlers for the selected protocol.
 */
public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {

  private Consumer<ChannelPipeline> h1FallbackPipelineAssembler;
  private final ImmutableMap<String, RequestHandler> handlers;

  Http2OrHttpHandler(Consumer<ChannelPipeline> fallBackpipelineAssembler, ImmutableMap<String, RequestHandler> handlers) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.h1FallbackPipelineAssembler = fallBackpipelineAssembler;
    this.handlers = handlers;
  }

  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      ctx.pipeline().addLast(new RestHttp2HandlerBuilder(this.handlers).build());
      return;
    }

    if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      h1FallbackPipelineAssembler.accept(ctx.pipeline());
      return;
    }

    throw new IllegalStateException("unknown protocol: " + protocol);
  }
}
