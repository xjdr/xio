package com.xjeffrose.xio.http;

import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.PromiseCombiner;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class Client {

  private final ClientState state;
  private final ClientChannelInitializer clientChannelInitializer;
  private final ChannelFutureListener connectionListener;
  private final ChannelFutureListener writeListener;
  private Channel channel;

  public Client(ClientState state, Supplier<ChannelHandler> appHandler, XioTracing tracing) {
    this.state = state;
    this.clientChannelInitializer = new ClientChannelInitializer(state, appHandler, tracing);

    connectionListener =
        f -> {
          if (f.isDone() && f.isSuccess()) {
            log.debug("Connection succeeded");
          } else {
            log.debug("Connection failed", f.cause());
          }
        };
    writeListener =
        f -> {
          if (f.isDone() && f.isSuccess()) {
            log.debug("Write succeeded");
          } else {
            log.debug("Write failed", f.cause());
            log.debug("pipeline: {}", channel.pipeline());
          }
        };
  }

  private ChannelFuture connect() {
    Bootstrap b = new Bootstrap();
    b.channel(state.channelConfig.channel());
    b.group(state.channelConfig.workerGroup());
    b.handler(clientChannelInitializer);
    return b.connect(state.remote);
  }

  public ChannelFuture write(Request request) {
    if (channel == null) {
      ChannelFuture future = connect();
      channel = future.channel();
      ChannelPromise promise = channel.newPromise();
      future.addListeners(connectionListener, new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isDone() && future.isSuccess()) {
            writeOperation(request, promise);
          } else {
            promise.setFailure(future.cause());
          }
        }
      });
      return promise;
    } else {
      return channel.writeAndFlush(request).addListener(writeListener);
    }
  }

  private void writeOperation(Request request, ChannelPromise promise) {
    val writeFuture = channel.writeAndFlush(request);
    writeFuture.addListeners(writeListener, new GenericFutureListener<Future<? super Void>>() {
      @Override
      public void operationComplete(Future<? super Void> future) throws Exception {
        if (future.isDone() && future.isSuccess()) {
          promise.setSuccess();
        } else {
          promise.setFailure(future.cause());
        }
      }
    });
  }
}
