package com.xjeffrose.xio.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientConnectionManager {
  private ClientChannelInitializer channelInitializer;
  private ChannelFutureListener releaseListener;
  private ChannelFutureListener connectionListener;
  private ChannelFuture currentChannelFuture;
  private ClientState state;
  private boolean reusable = true;
  private ClientConnectionState connectionState = ClientConnectionState.NOT_CONNECTED;

  Channel currentChannel() {
    if (currentChannelFuture != null) {
      return currentChannelFuture.channel();
    }
    return null;
  }

  public ClientConnectionManager(ClientState state, ClientChannelInitializer channelInitializer) {
    this.state = state;
    this.channelInitializer = channelInitializer;

    releaseListener =
        f -> {
          log.debug("Channel closed");
          connectionState = ClientConnectionState.CLOSED_CONNECTION;
          this.currentChannelFuture = null;
          this.reusable = false;
        };
    connectionListener =
        f -> {
          if (f.isDone() && f.isSuccess()) {
            log.debug("Connection succeeded");
            connectionState = ClientConnectionState.CONNECTED;
          } else {
            log.debug("Connection failed", f.cause());
            connectionState = ClientConnectionState.CLOSED_CONNECTION;
            this.currentChannelFuture = null;
            this.reusable = false;
          }
        };
  }

  public ChannelFuture connect() {
    if (connectionState == ClientConnectionState.NOT_CONNECTED) {
      connectionState = ClientConnectionState.CONNECTING;

      Bootstrap bootstrap = new Bootstrap();
      bootstrap.channel(state.channelConfig.channel());
      bootstrap.group(state.channelConfig.workerGroup());
      bootstrap.handler(channelInitializer);
      ChannelFuture connectFuture = bootstrap.connect(state.remote);
      currentChannelFuture = connectFuture;
      currentChannelFuture.channel().closeFuture().addListener(releaseListener);
      connectFuture.addListener(connectionListener);
      return connectFuture;
    } else {
      return currentChannelFuture;
    }
  }

  public ClientConnectionState connectionState() {
    return connectionState;
  }

  public boolean isReusable() {
    return reusable;
  }

  public void setBackendHandlerSupplier(Supplier<ChannelHandler> handlerSupplier) {
    channelInitializer.setAppHandler(handlerSupplier);
  }
}
