package com.xjeffrose.xio.processor;

import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.server.RequestContext;
import java.util.Map;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;

public interface XioProcessor {
  ListenableFuture<Boolean> process(ChannelHandlerContext ctx, HttpRequest req, RequestContext respCtx);

  // NioSocketChannel nioSocketChannel = (NioSocketChannel) ctx.getChannel();
  // nioSocketChannel.getWorker().executeInIoThread(runnable, true);
  public void executeInIoThread(Runnable runnable);
}

