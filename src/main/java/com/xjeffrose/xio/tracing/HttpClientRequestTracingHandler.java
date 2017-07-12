package com.xjeffrose.xio.tracing;

import brave.Span;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;

class HttpClientRequestTracingHandler extends ChannelOutboundHandlerAdapter {

  private final HttpClientTracingState state;

  public HttpClientRequestTracingHandler(HttpClientTracingState state) {
    super();
    this.state = state;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (!(msg instanceof HttpRequest)) {
      ctx.write(msg, promise);
      return;
    }

    HttpRequest request = (HttpRequest) msg;
    Span span = state.onRequest(ctx, request);

    ctx.write(msg, promise).addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (!future.isSuccess()) {
            //System.out.println("ERROR: " + future.cause());
            state.onError(ctx, future.cause());
          }
        }
      });
  }

}
