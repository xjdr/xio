package com.xjeffrose.xio.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.internal.PlatformDependent;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.timeout.IdleStateEvent.ALL_IDLE_STATE_EVENT;

@Slf4j
public class XioService extends ChannelDuplexHandler {

  private final Deque<ChannelHandler> serviceList = PlatformDependent.newConcurrentDeque();

  boolean blockActive = false;
  boolean blockInactive = false;
  boolean blockRead = false;
  boolean blockReadComplete = false;
  boolean blockWrite = false;

  public XioService() {

  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object _evt) {
    XioEvent evt;

    if (_evt instanceof XioEvent) {
      evt = (XioEvent) _evt;

      switch (evt) {
        case BLOCK_ACTIVE:
          blockActive = true;
          break;
        case BLOCK_INACTIVE:
          blockInactive = true;
          break;
        case BLOCK_READ:
          blockRead = true;
          ctx.channel().config().setAutoRead(false);
          break;
        case BLOCK_READCOMPLETE:
          blockReadComplete = true;
          break;
        case BLOCK_WRITE:
          blockWrite = true;
          break;
        case UNBLOCK_ACTIVE:
          blockActive = false;
          ctx.fireChannelActive();
          break;
        case UNBLOCK_INACTIVE:
          blockInactive = false;
          ctx.fireChannelInactive();
          break;
        case UNBLOCK_READ:
          blockRead = false;
          ctx.channel().config().setAutoRead(true);
//        ctx.fireChannelRead();
          break;
        case UNBLOCK_READCOMPLETE:
          blockReadComplete = false;
          ctx.fireChannelReadComplete();
          break;
        case UNBLOCK_WRITE:
          blockWrite = false;
          // Is this the correct event?
          ctx.fireChannelWritabilityChanged();
          break;
        case REQUEST_ERROR:
          break;
        case REQUEST_SENT:
          break;
        case REQUEST_SUCCESS:
          break;
        case RESPONSE_ERROR:
          break;
        case RESPONSE_RECIEVED:
          break;
        case RESPONSE_SUCCESS:
          break;
        default:
          break;
      }
    } else if (_evt instanceof IdleStateEvent) {
      IdleStateEvent idleStateEvent = (IdleStateEvent) _evt;

      IdleState state = idleStateEvent.state();

    } else {

    }
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
    if (!blockActive) {
      ctx.fireChannelActive();
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (!blockInactive) {
      ctx.fireChannelInactive();
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (!blockRead) {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    if (!blockReadComplete) {
      ctx.fireChannelReadComplete();
    }
  }

  public XioService handler(ChannelHandler handler) {
    serviceList.addFirst(handler);

    return this;
  }

  public XioService andThen(ChannelHandler handler) {
    serviceList.addLast(handler);

    return this;
  }

  public Deque<ChannelHandler> getServiceList() {
    return serviceList;
  }
}
