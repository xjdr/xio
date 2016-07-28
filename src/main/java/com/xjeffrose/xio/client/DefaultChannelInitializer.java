package com.xjeffrose.xio.client;

import com.xjeffrose.xio.SSL.XioSecurityHandlerImpl;
import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;

public class DefaultChannelInitializer extends ChannelInitializer {

  private final ChannelHandler handler;
  private final boolean ssl;

  public DefaultChannelInitializer(ChannelHandler handler, boolean ssl) {
    this.handler = handler;
    this.ssl = ssl;
  }

  public ChannelHandler protocolHandler() {
    return new HttpClientCodec();
  }

  @Override
  public void initChannel(Channel channel) {
    ChannelPipeline cp = channel.pipeline();
    if (ssl) {
      cp.addLast("encryptionHandler", new XioSecurityHandlerImpl(true).getEncryptionHandler());
    }
    cp.addLast("protocolHandler", protocolHandler());
    cp.addLast(new XioIdleDisconnectHandler(60, 60, 60));
    cp.addLast(handler);
  }

}
