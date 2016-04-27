package com.xjeffrose.xio.proxy;

import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.core.ConnectionStateTracker;
import com.xjeffrose.xio.core.Constants;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Attribute;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.log4j.Logger;

public class XioChannelProxy extends ChannelDuplexHandler {
  private static final Logger log = Logger.getLogger(XioChannelProxy.class);
  private final Channel inboundChannel;
  private final ConnectionStateTracker connectionContext;
  private SslHandler sslHandler;

  // set during channelActive
  private Node node;

  // logging purpose
  private long timeActive;

  public XioChannelProxy(Channel inboundChannel) {
    this.inboundChannel = inboundChannel;
    connectionContext = inboundChannel.attr(Constants.CONNECTION_STATE_TRACKER).get();
  }

  static void closeOnFlush(Channel ch) {
    if (ch.isOpen()) {
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    timeActive = System.currentTimeMillis();
    if (log.isDebugEnabled()) {
      log.debug("Service channelActive " + inboundChannel + " " + ctx.channel());
    }
    // attr never returns null
    Attribute<Node> attrNode = inboundChannel.attr(Constants.PICKED_OUTBOUND_NODE);
    node = attrNode.get();
    if (node != null) {
      node.addPending(ctx.channel());
    }
    sslHandler = ctx.channel().pipeline().get(SslHandler.class);
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, Object msg) {
    connectionContext.incrementReadCount(ctx.channel());

    if (log.isDebugEnabled()) {
      log.debug("Service channelRead and WRITE back " + inboundChannel + " " + ctx.channel());
    }
    inboundChannel.write(msg).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) {
        if (!future.isSuccess()) {
          log.error(connectionContext.toString("WRITE back failure: ",
              future.cause() != null ? future.cause().getMessage() : null), future.cause());
          future.channel().close();
        }
      }
    });
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("Service channelReadComplete " + inboundChannel + " " + ctx.channel());
    }
    inboundChannel.flush();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (node != null) {
      node.removePending(ctx.channel());
    }
    long duration = System.currentTimeMillis() - timeActive;
    String str = connectionContext.toString("", " duration: " + duration);
    if (connectionContext.getReadCount(ctx.channel()) == 0) {
      log.warn(str);
    } else {
      log.info(str);
    }
    closeOnFlush(inboundChannel);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error(connectionContext.toString("Exception caught: ",
        cause.getClass().getName() + " " + cause.getMessage()), cause);
    closeOnFlush(ctx.channel());
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    // non ssl or strict pass-through case
    if (sslHandler == null) {
      ctx.write(msg, promise);
      return;
    }

    sslHandler.handshakeFuture().addListener(new GenericFutureListener<Future<Channel>>() {
      @Override
      public void operationComplete(Future<Channel> future) throws Exception {
        if (future.isSuccess()) {
          ctx.write(msg, promise);
        }
      }
    });
  }
}
