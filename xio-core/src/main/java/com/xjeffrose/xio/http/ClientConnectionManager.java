package com.xjeffrose.xio.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientConnectionManager {
  private ClientState state;
  private ClientChannelInitializer channelInitializer;
  private ChannelFutureListener releaseListener;
  private ChannelFutureListener connectionListener;
  private ChannelFuture currentChannelFuture;
  private Channel currentChannel;
  private ClientConnectionState connectionState = ClientConnectionState.NOT_CONNECTED;

  Channel currentChannel() {
    return currentChannel;
  }

  ClientConnectionManager(ClientState state, ClientChannelInitializer channelInitializer) {
    this.state = state;
    this.channelInitializer = channelInitializer;

    releaseListener =
      f -> {
        log.debug("Channel closed");
        connectionState = ClientConnectionState.NOT_CONNECTED;
        this.currentChannel = null;
        this.currentChannelFuture = null;
      };
    connectionListener =
      f -> {
        if (f.isDone() && f.isSuccess()) {
          log.debug("Connection succeeded");
          connectionState = ClientConnectionState.CONNECTED;
        } else {
          log.debug("Connection failed", f.cause());
          connectionState = ClientConnectionState.FAILED_CONNECTION;
        }
      };
  }

  ChannelFuture connect() {
    if (connectionState == ClientConnectionState.NOT_CONNECTED) {
      connectionState = ClientConnectionState.CONNECTING;

      Bootstrap b = new Bootstrap();
      b.channel(state.channelConfig.channel());
      b.group(state.channelConfig.workerGroup());
      b.handler(channelInitializer);
      ChannelFuture connectFuture = b.connect(state.remote);
      currentChannelFuture = connectFuture;
      currentChannel = currentChannelFuture.channel();
      currentChannel.closeFuture().addListener(releaseListener);
      connectFuture.addListener(connectionListener);
      return connectFuture;
    }
    else {
      return currentChannelFuture;
    }
  }

  ClientConnectionState connectionState() {
    return connectionState;
  }

}

