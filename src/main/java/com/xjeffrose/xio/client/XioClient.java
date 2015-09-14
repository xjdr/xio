package com.xjeffrose.xio.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class XioClient {

  private final ChannelHandlerContext ctx;
  private final String host;
  private final int port;

  public XioClient(String host, int port) {

    this.ctx = null;
    this.host = host;
    this.port = port;
  }

  public XioClient(ChannelHandlerContext ctx, String host, int port) {

    this.ctx = ctx;
    this.host = host;
    this.port = port;
  }

  public void execute() {

  }

  private void connect() {
    Bootstrap bootstrap = new Bootstrap();
    if (ctx != null) {
      bootstrap
          .group(ctx.channel().eventLoop())
          .channel(NioSocketChannel.class);
    } else {
      bootstrap
          .group(new NioEventLoopGroup())
          .channel(NioSocketChannel.class);
    }

//    bootstrap.handler();

//    if (connectTimeout != null) {
//      bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
//    }

    // Set some sane defaults
    bootstrap
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
        .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
        .option(ChannelOption.TCP_NODELAY, true);

    ChannelFuture nettyChannelFuture = bootstrap.connect();

    nettyChannelFuture.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
          // do something
        } else {
          // do something
        }
      }
    });
  }
}
