package com.xjeffrose.xio.client.loadbalancer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import java.net.SocketAddress;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnpooledNode extends Node {

  private final Bootstrap bootstrap;
  private ChannelFuture channelResult;

  public UnpooledNode(SocketAddress address, Bootstrap bootstrap) {
    super(address, bootstrap);
    this.bootstrap = bootstrap;
  }

  private void writeAndFlush(Object message, DefaultPromise<Void> promise) {
    Channel channel = channelResult.channel();
    channel
        .writeAndFlush(message)
        .addListener(
            (ChannelFutureListener)
                channelFuture -> {
                  if (channelFuture.isSuccess()) {
                    log.debug("write finished for " + message);
                    promise.setSuccess(null);
                  } else {
                    log.error("Write error: ", channelFuture.cause());
                    promise.setFailure(channelFuture.cause());
                  }
                });
  }

  public Future<Void> send(Object message) {
    DefaultPromise<Void> promise = new DefaultPromise<>(eventLoopGroup().next());

    log.debug("Acquiring Node: " + this);
    if (channelResult == null) {
      channelResult = bootstrap.clone().connect();
    }

    if (channelResult.isSuccess()) {
      writeAndFlush(message, promise);
    } else {
      channelResult.addListener(
          (ChannelFutureListener)
              channelFuture -> {
                if (channelFuture.isSuccess()) {
                  log.debug("connection achieved " + message);
                  writeAndFlush(message, promise);
                } else {
                  log.error("connection error: ", channelFuture.cause());
                  promise.setFailure(channelFuture.cause());
                }
              });
    }

    return promise;
  }
}
