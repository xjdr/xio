package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.xjeffrose.xio.core.FrameLengthCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

abstract public class RequestMuxerConnector {

  private final SocketAddress address;

  private final Bootstrap baseBootstrap;

  private RequestMuxerConnector(SocketAddress address) {
    this.address = address;
    baseBootstrap = buildBootstrap();
  }

  public RequestMuxerConnector(InetSocketAddress address) {
    this((SocketAddress)address);
  }

  public RequestMuxerConnector(LocalAddress address) {
    this((SocketAddress)address);
  }

  abstract protected ChannelHandler responseHandler();

  protected ChannelHandler handler() {
    return new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel channel) {
        channel.pipeline()
          //.addLast(new LoggingHandler(LogLevel.ERROR))
          .addLast("frame length codec", new FrameLengthCodec())
          .addLast("muxing protocol codec", new RequestMuxerCodec())
          .addLast("response handler", responseHandler())
          ;
      }
    };
  }

  abstract protected EventLoopGroup group();

  abstract protected Class<? extends Channel> channel();

  private Bootstrap buildBootstrap() {
    return new Bootstrap()
      // TODO(CK): move all of these constants out into Config
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
      .option(ChannelOption.SO_REUSEADDR, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
      .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
      .option(ChannelOption.TCP_NODELAY, true)
      .group(group())
      .channel(channel());
  }

  protected Bootstrap cloneBootstrap() {
    return baseBootstrap.clone().handler(handler());
  }

  protected void connectBootstrap(SettableFuture<Channel> promise) {
    cloneBootstrap().connect(address).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
          promise.set(future.channel());
        } else {
          promise.setException(future.cause());
        }
      }
    });
  }

  public ListenableFuture<Channel> connect() {
    SettableFuture<Channel> promise = SettableFuture.create();
    connectBootstrap(promise);
    return promise;
  }

}
