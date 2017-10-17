package com.xjeffrose.xio.tracing;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;

class HttpServerResponseTracingHandler extends ChannelOutboundHandlerAdapter {

  private final HttpServerTracingState state;

  public HttpServerResponseTracingHandler(HttpServerTracingState state) {
    super();
    this.state = state;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (!(msg instanceof HttpResponse)) {
      ctx.write(msg, promise);
      return;
    }

    final HttpResponse response = (HttpResponse) msg;

    ctx.write(msg, promise).addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          state.onResponse(ctx, response, future.cause());
        }
      });
  }

}
