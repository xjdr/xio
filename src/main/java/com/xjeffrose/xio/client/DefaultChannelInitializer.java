package com.xjeffrose.xio.client;

import static com.xjeffrose.xio.pipeline.Pipelines.addHandler;

import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

public class DefaultChannelInitializer extends ChannelInitializer {

  private final ClientConfig config;
  private final InetSocketAddress address;
  private final ChannelHandler handler;
  private final SslContext sslContext;
  private final Supplier<ChannelHandler> applicationProtocol;
  private final Supplier<ChannelHandler> tracingHandler;

  public DefaultChannelInitializer(ClientState state) {
    this.config = state.config;
    this.address = state.address;
    this.handler = state.handler;
    this.sslContext = state.sslContext;
    this.applicationProtocol = state.applicationProtocol;
    this.tracingHandler = state.tracingHandler;
  }

  @Override
  public void initChannel(Channel channel) {
    ChannelPipeline cp = channel.pipeline();
    if (sslContext != null) {
      cp.addLast("encryptionHandler", sslContext.newHandler(channel.alloc(), address.getHostString(), address.getPort()));
    }
    if (config.isMessageLoggerEnabled()) {
      addHandler(cp, "message logging", new XioMessageLogger(XioClient.class, config.getName()));
    }
    addHandler(cp, "protocol handler", applicationProtocol.get());
    addHandler(cp, "distributed tracing", tracingHandler.get());
    addHandler(cp, "request encoder", new XioRequestEncoder());
    cp.addLast(new XioIdleDisconnectHandler(60, 60, 60));
    cp.addLast(handler);
  }

}
