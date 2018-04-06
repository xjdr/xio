package com.xjeffrose.xio.http;

import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPromise;
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
  private ChannelFuture connectionFuture;

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
      connectionFuture = connect();
      channel = connectionFuture.channel();
      ChannelPromise promise = channel.newPromise();
      connectionFuture.addListeners(
          connectionListener,
          (resultFuture) -> {
            if (resultFuture.isDone() && resultFuture.isSuccess()) {
              writeOperation(request, promise);
            } else {
              promise.setFailure(resultFuture.cause());
            }
          });
      return promise;
    } else {
      if (connectionFuture.isDone() && connectionFuture.isSuccess()) {
        return channel.writeAndFlush(request).addListener(writeListener);
      } else {
        ChannelPromise promise = channel.newPromise();
        connectionFuture.addListener(
            (resultFuture) -> {
              if (resultFuture.isDone() && resultFuture.isSuccess()) {
                writeOperation(request, promise);
              } else {
                promise.setFailure(resultFuture.cause());
              }
            });
        return promise;
      }
    }
  }

  private void writeOperation(Request request, ChannelPromise promise) {
    val writeFuture = channel.writeAndFlush(request);
    writeFuture.addListeners(
        writeListener,
        (resultFuture) -> {
          if (resultFuture.isDone() && resultFuture.isSuccess()) {
            promise.setSuccess();
          } else {
            promise.setFailure(resultFuture.cause());
          }
        });
  }
}
