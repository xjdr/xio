package com.xjeffrose.xio.processor;

import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.server.RequestContext;
import com.xjeffrose.xio.server.XioServerConfig;
import java.util.Map;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;

public interface XioProcessor {
  ListenableFuture<Boolean> process(ChannelHandlerContext ctx, XioServerConfig config, HttpRequest req, RequestContext respCtx);

  void executeInIoThread(ChannelHandlerContext ctx, Runnable runnable);
}

