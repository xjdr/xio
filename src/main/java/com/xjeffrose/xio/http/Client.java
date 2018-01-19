package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.core.XioMessageLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.PromiseCombiner;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Client {

  private final ClientState state;
  private final Supplier<ChannelHandler> appHandler;
  private final ChannelFutureListener connectionListener;
  private final ChannelFutureListener writeListener;
  private Channel channel;

  public Client(ClientState state, Supplier<ChannelHandler> appHandler) {
    this.state = state;
    this.appHandler = appHandler;
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

  private ChannelHandler buildHttp2Handler() {
    return new Http2HandlerBuilder().server(false).build();
  }

  private ChannelFuture connect() {
    Bootstrap b = new Bootstrap();
    b.channel(state.channelConfig.channel());
    b.group(state.channelConfig.workerGroup());
    b.handler(
        new ChannelInitializer() {
          public void initChannel(Channel channel) {
            if (state.sslContext != null) {
              channel
                  .pipeline()
                  .addLast(
                      "ssl handler",
                      state.sslContext.newHandler(
                          channel.alloc(), state.remote.getHostString(), state.remote.getPort()));
            }
            channel
                .pipeline()
                .addLast(
                    "negotiation handler",
                    new HttpClientNegotiationHandler(Client.this::buildHttp2Handler))
                .addLast("codec", CodecPlaceholderHandler.INSTANCE)
                .addLast("application codec", ApplicationCodecPlaceholderHandler.INSTANCE)
                .addLast("idle handler", new XioIdleDisconnectHandler(60, 60, 60))
                .addLast("message logging", new XioMessageLogger(Client.class, "objects"))
                .addLast("request buffer", new RequestBuffer())
                .addLast("app handler", appHandler.get());
          }
        });

    return b.connect(state.remote);
  }

  public ChannelFuture write(Request request) {
    if (channel == null) {
      ChannelFuture future = connect();
      channel = future.channel();
      ChannelPromise promise = channel.newPromise();
      PromiseCombiner combiner = new PromiseCombiner();
      combiner.add(future.addListener(connectionListener));
      combiner.add(channel.writeAndFlush(request).addListener(writeListener));
      combiner.finish(promise);
      return promise;
    } else {
      return channel.writeAndFlush(request).addListener(writeListener);
    }
  }
}
