package com.xjeffrose.xio.http;

import com.xjeffrose.xio.SSL.SslContextFactory;
import com.xjeffrose.xio.client.ChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.http.Http1ClientCodec;
import com.xjeffrose.xio.http.PipelineRequestHandler;
import com.xjeffrose.xio.http.RawBackendHandler;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.Route;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;
import io.netty.channel.ChannelHandler;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import java.util.function.Supplier;
import java.util.concurrent.Future;
import io.netty.channel.ChannelFutureListener;

public class Client {

  private final ClientState state;
  private final Supplier<ChannelHandler> appHandler;
  Channel channel;

  public Client(ClientState state, Supplier<ChannelHandler> appHandler) {
    this.state = state;
    this.appHandler = appHandler;
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
            //              .addLast(new RawBackendHandler(ctx));
          }
        });

    return b.connect(state.remote);
  }

  private void connected(ChannelFuture f) {
    if (f.isDone() && f.isSuccess()) {
      // log?
    } else {
      // log?
      throw new RuntimeException(f.cause());
    }
  }

  public ChannelFuture write(Request request) {
    if (channel == null) {
      ChannelFuture future = connect();
      ChannelFutureListener l =
          f -> {
            if (f.isDone() && f.isSuccess()) {
              // log?
            } else {
              // log?
              // fail the write future?
              throw new RuntimeException(f.cause());
            }
          };
      channel = future.channel();
      // connect().addListener(f -> connected((ChannelFuture) f));
      connect().addListener(l);
    }
    return channel.writeAndFlush(request);
  }
}
