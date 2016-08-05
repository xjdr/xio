package com.xjeffrose.xio.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Handler implementation for the tcp proxy server.
 */
public class TcpProxyCodec extends ChannelInboundHandlerAdapter {

  public class BackendProxyCodec extends ChannelInboundHandlerAdapter {

    private final Channel inboundChannel;

    public BackendProxyCodec(Channel inboundChannel) {
      this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
      inboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
          if (future.isSuccess()) {
            ctx.channel().read();
          } else {
            future.channel().close();
          }
        }
      });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      log.error("Error: ", cause);
      closeOnFlush(ctx.channel());
    }

    void closeOnFlush(Channel ch) {
      if (ch.isActive()) {
        ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  private static final Logger log = LoggerFactory.getLogger(EchoCodec.class);

  private final InetSocketAddress proxyEndpoint;

  public TcpProxyCodec(InetSocketAddress proxyEndpoint) {
    this.proxyEndpoint = proxyEndpoint;
  }

  private volatile Channel outboundChannel;

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    final Channel inboundChannel = ctx.channel();
    inboundChannel.config().setAutoRead(false);

    // Start the connection attempt.
    Bootstrap b = new Bootstrap();
    b.group(inboundChannel.eventLoop())
      .channel(ctx.channel().getClass())
      .handler(new BackendProxyCodec(inboundChannel))
      .option(ChannelOption.AUTO_READ, false);
    ChannelFuture f = b.connect(proxyEndpoint);
    outboundChannel = f.channel();
    f.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
          // connection complete start to read first data
          inboundChannel.read();
        } else {
          // Close the connection if the connection attempt has failed.
          inboundChannel.close();
        }
      }
    });
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, Object msg) {
    if (outboundChannel.isOpen()) {
      outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
          if (future.isSuccess()) {
            // was able to flush out data, start to read the next chunk
            ctx.channel().read();
          } else {
            future.channel().close();
          }
        }
      });
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (outboundChannel != null) {
      closeOnFlush(outboundChannel);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    log.error("Error: ", cause);
    closeOnFlush(ctx.channel());
  }

  /**
   * Closes the specified channel after all queued write requests are flushed.
   */
  void closeOnFlush(Channel ch) {
    if (ch.isActive()) {
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

}
