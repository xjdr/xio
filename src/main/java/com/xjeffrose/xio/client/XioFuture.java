package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.AbstractFuture;
import io.airlift.units.Duration;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import javax.annotation.Nullable;


class XioFuture<T extends XioClientChannel> extends AbstractFuture<T> {
  protected XioFuture(final XioClientConnector<T> clientChannelConnector,
                      @Nullable final Duration receiveTimeout,
                      @Nullable final Duration readTimeout,
                      @Nullable final Duration sendTimeout,
                      final ChannelFuture channelFuture,
                      final XioClientConfig xioClientConfig) {
    channelFuture.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        try {
          if (future.isSuccess()) {
            Channel nettyChannel = future.channel();
            T channel = clientChannelConnector.newClientChannel(nettyChannel, xioClientConfig);
            channel.setReceiveTimeout(receiveTimeout);
            channel.setReadTimeout(readTimeout);
            channel.setSendTimeout(sendTimeout);
            XioFuture.this.set(channel);

          } else if (future.isCancelled()) {
            if (!cancel(true)) {
              setException(new XioTransportException("Unable to cancel client channel connection"));
            }
          } else {
            throw future.cause();
          }
        } catch (Throwable t) {
          setException(new XioTransportException("Failed to connect client channel", t));
        }
      }
    });
  }
}

