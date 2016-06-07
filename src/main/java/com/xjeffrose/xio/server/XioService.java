package com.xjeffrose.xio.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.Logger;

public class XioService extends ChannelDuplexHandler {
  private static final Logger log = Logger.getLogger(XioService.class.getName());

  private final ConcurrentLinkedDeque<ChannelHandler> serviceList = new ConcurrentLinkedDeque<>();

  public XioService() {

  }

  @Override
  @SuppressWarnings("deprecated")
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    serviceList.stream().forEach(xs -> {
        ctx.pipeline().addLast(xs);
    });
    ctx.pipeline().remove(this);
    ctx.fireChannelActive();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.fireChannelReadComplete();
  }

  public XioService handler(ChannelHandler handler) {
    serviceList.addFirst(handler);

    return this;
  }

  public XioService andThen(ChannelHandler handler) {
    serviceList.addLast(handler);

    return this;
  }

  public ConcurrentLinkedDeque<ChannelHandler> getServiceList() {
    return serviceList;
  }
}
