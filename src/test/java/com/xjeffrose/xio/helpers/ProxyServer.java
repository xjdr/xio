package com.xjeffrose.xio.helpers;

import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;

abstract class ProxyServer implements XioPipelineFragment {

  protected final InetSocketAddress destination;

  protected ProxyServer(InetSocketAddress destination) {
    this.destination = destination;
  }

  public String applicationProtocol() {
    return null;
  }

  protected abstract class IntermediaryHandler extends SimpleChannelInboundHandler<Object> {
    private final Queue<Object> received = new ArrayDeque<Object>();

    private boolean finished;
    private Channel backend;

    public void addReceived(Object msg) {
      received.add(ReferenceCountUtil.retain(msg));
    }

    @Override
    protected final void channelRead0(final ChannelHandlerContext ctx, Object msg) throws Exception {
      if (finished) {
        received.add(ReferenceCountUtil.retain(msg));
        flush();
        return;
      }

      boolean finished = handleProxyProtocol(ctx, msg);
      if (finished) {
        this.finished = true;
        ChannelFuture f = connectToDestination(ctx.channel().eventLoop(), new BackendHandler(ctx));
        f.addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
              ctx.close();
            } else {
              backend = future.channel();
              flush();
            }
          }
        });
      }
    }

    private void flush() {
      if (backend != null) {
        boolean wrote = false;
        for (;;) {
          Object msg = received.poll();
          if (msg == null) {
            break;
          }
          backend.write(msg);
          wrote = true;
        }

        if (wrote) {
          backend.flush();
        }
      }
    }

    protected abstract boolean handleProxyProtocol(ChannelHandlerContext ctx, Object msg) throws Exception;

    protected abstract SocketAddress intermediaryDestination();

    protected ChannelFuture connectToDestination(EventLoop loop, ChannelHandler handler) {
      Bootstrap b = new Bootstrap();
      b.channel(NioSocketChannel.class);
      b.group(loop);
      b.handler(handler);
      return b.connect(intermediaryDestination());
    }

    @Override
    public final void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      if (backend != null) {
        backend.close();
      }
    }

    @Override
    public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      ctx.close();
    }

    private final class BackendHandler extends ChannelInboundHandlerAdapter {

      private final ChannelHandlerContext frontend;

      BackendHandler(ChannelHandlerContext frontend) {
        this.frontend = frontend;
      }

      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        frontend.write(msg);
      }

      @Override
      public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        frontend.flush();
      }

      @Override
      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        frontend.close();
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
      }
    }
  }
}
