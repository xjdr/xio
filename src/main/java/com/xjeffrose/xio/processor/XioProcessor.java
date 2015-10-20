package com.xjeffrose.xio.processor;

import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.server.RequestContext;
import io.netty.channel.ChannelHandlerContext;

public interface XioProcessor {
  void connect(ChannelHandlerContext ctx);

  void disconnect(ChannelHandlerContext ctx);

  ListenableFuture<Boolean> process(ChannelHandlerContext ctx, Object request, RequestContext reqCtx);
}

