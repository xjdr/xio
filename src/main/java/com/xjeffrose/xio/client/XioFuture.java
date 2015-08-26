package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.AbstractFuture;
import io.airlift.units.Duration;
import javax.annotation.Nullable;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

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
            Channel nettyChannel = future.getChannel();
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
            throw future.getCause();
          }
        } catch (Throwable t) {
          setException(new XioTransportException("Failed to connect client channel", t));
        }
      }
    });
  }
}

