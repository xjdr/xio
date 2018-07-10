package com.xjeffrose.xio.http;

import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.util.concurrent.PromiseCombiner;
import java.net.InetSocketAddress;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Client {

  private final ClientState state;
  private final ClientChannelInitializer clientChannelInitializer;
  private final ChannelFutureListener connectionListener;
  private final ChannelFutureListener writeListener;
  private final ChannelFutureListener releaseListener;
  private Channel channel;
  private boolean reusable = true;

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
            if (channel != null) {
              log.debug("pipeline: {}", channel.pipeline());
            }
          }
        };
    releaseListener =
        f -> {
          log.debug("Channel closed");
          channel = null;
          reusable = false;
        };
  }

  public InetSocketAddress remoteAddress() {
    return state.remote;
  }

  /**
   * Creates a channel and returns a channel connection future This also sets the channel instance
   * variable so if we later call write it will not try to reconnect
   *
   * @return A ChannelFuture that succeeds on connect
   */
  public ChannelFuture connect() {
    Bootstrap b = new Bootstrap();
    b.channel(state.channelConfig.channel());
    b.group(state.channelConfig.workerGroup());
    b.handler(clientChannelInitializer);
    ChannelFuture connectFuture = b.connect(state.remote);
    channel = connectFuture.channel();
    channel.closeFuture().addListener(releaseListener);
    return connectFuture;
  }

  /**
   * Combines the connection and writing into one command. This method dispatches both a connect and
   * command call concurrently. If there is already an existing channel we just do the write
   *
   * @param request The Request object that we ultimately want to send outbound
   * @return A ChannelFuture that succeeds when both the connect and write succeed
   */
  public ChannelFuture write(Request request) {
    if (channel == null) {
      ChannelFuture future = connect();
      ChannelPromise promise = channel.newPromise();
      PromiseCombiner combiner = new PromiseCombiner();
      combiner.add(future.addListener(connectionListener));
      if (request.endOfMessage()) {
        combiner.add(channel.writeAndFlush(request).addListener(writeListener));
      } else {
        combiner.add(channel.write(request).addListener(writeListener));
      }
      combiner.finish(promise);
      return promise;
    } else {
      if (request.endOfMessage()) {
        return channel.writeAndFlush(request).addListener(writeListener);
      } else {
        return channel.write(request).addListener(writeListener);
      }
    }
  }

  public void prepareForReuse(Supplier<ChannelHandler> handlerSupplier) {
    clientChannelInitializer.setAppHandler(handlerSupplier);
    if (channel != null) {
      channel.pipeline().addLast(ClientChannelInitializer.APP_HANDLER, handlerSupplier.get());
    }
  }

  public void recycle() {
    if (channel != null) {
      channel.pipeline().remove(ClientChannelInitializer.APP_HANDLER);
      Http2ClientStreamMapper.http2ClientStreamMapper(channel.pipeline().firstContext()).clear();
    }
  }

  /**
   * @return if true this {@link Client }is reusable or false if when the channel was closed and we
   *     do not want to reconnect since the the {@link ClientState}'s worker group will be
   *     incorrect.
   */
  public boolean isReusable() {
    return reusable;
  }
}
