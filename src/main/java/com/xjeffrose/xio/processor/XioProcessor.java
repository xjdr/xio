package com.xjeffrose.xio.processor;

import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.server.RequestContext;
import com.xjeffrose.xio.server.XioServerConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

public interface XioProcessor {
  ListenableFuture<Boolean> process(ChannelHandlerContext ctx, HttpRequest request, RequestContext reqCtx);
}

