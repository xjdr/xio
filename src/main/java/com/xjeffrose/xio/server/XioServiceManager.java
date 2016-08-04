package com.xjeffrose.xio.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedDeque;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XioServiceManager extends ChannelDuplexHandler {


  private final ConcurrentLinkedDeque<XioService> serviceList = new ConcurrentLinkedDeque<>();

  private final XioService xioService;

  public XioServiceManager(XioService xioService) {
    this.xioService = xioService;
  }

  @Override
  @SuppressWarnings("deprecated")
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

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

  public void andThen(XioService xioService) {
    serviceList.addLast(xioService);
  }

  public ConcurrentLinkedDeque<XioService> getServiceList() {
    return serviceList;
  }
}
