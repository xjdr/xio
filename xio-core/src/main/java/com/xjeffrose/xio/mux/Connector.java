package com.xjeffrose.xio.mux;

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
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.local.LocalAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class Connector {

  private final SocketAddress address;

  private Connector(SocketAddress address) {
    this.address = address;
  }

  public Connector(InetSocketAddress address) {
    this((SocketAddress) address);
  }

  public Connector(LocalAddress address) {
    this((SocketAddress) address);
  }

  protected List<Map.Entry<String, ChannelHandler>> payloadHandlers() {
    return Arrays.asList();
  }

  // TODO(CK): get this from the constructor?
  protected ChannelHandler handler() {
    return new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline
            .addLast("frame length codec", new FrameLengthCodec())
            .addLast("mux message codec", new Codec());
        for (Map.Entry<String, ChannelHandler> entry : payloadHandlers()) {
          pipeline.addLast(entry.getKey(), entry.getValue());
        }
        pipeline.addLast("mux client codec", new ClientCodec());
      }
    };
  }

  protected abstract EventLoopGroup group();

  protected abstract Class<? extends Channel> channel();

  protected Bootstrap configure(Bootstrap bootstrap) {
    return bootstrap;
  }

  private Bootstrap buildBootstrap() {
    Bootstrap bootstrap = new Bootstrap();
    // TODO(CK): move all of these constants out into Config
    bootstrap
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(
            ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
        .option(ChannelOption.AUTO_READ, true)
        .group(group())
        .channel(channel());
    return configure(bootstrap);
  }

  protected Bootstrap cloneBootstrap() {
    return buildBootstrap().clone().handler(handler());
  }

  protected void connectBootstrap(SettableFuture<Channel> promise) {
    cloneBootstrap()
        .connect(address)
        .addListener(
            new ChannelFutureListener() {
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

  public SocketAddress address() {
    return address;
  }
}
