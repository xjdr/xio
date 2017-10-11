package com.xjeffrose.xio.client;

import static com.xjeffrose.xio.pipeline.Pipelines.addHandler;

import com.xjeffrose.xio.SSL.XioSecurityHandlerImpl;
import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import java.util.function.Supplier;

public class DefaultChannelInitializer extends ChannelInitializer {

  private final ChannelHandler handler;
  private final boolean ssl;
  private final Supplier<ChannelHandler> applicationProtocol;
  private final Supplier<ChannelHandler> tracingHandler;

  public DefaultChannelInitializer(ChannelHandler handler, boolean ssl, Supplier<ChannelHandler> applicationProtocol, Supplier<ChannelHandler> tracingHandler) {
    this.handler = handler;
    this.ssl = ssl;
    this.applicationProtocol = applicationProtocol;
    this.tracingHandler = tracingHandler;
  }

  @Override
  public void initChannel(Channel channel) {
    ChannelPipeline cp = channel.pipeline();
    if (ssl) {
      cp.addLast("encryptionHandler", new XioSecurityHandlerImpl(true).getEncryptionHandler());
    }
    addHandler(cp, "protocol handler", applicationProtocol.get());
    addHandler(cp, "distributed tracing", tracingHandler.get());
    addHandler(cp, "request encoder", new XioRequestEncoder());
    cp.addLast(new XioIdleDisconnectHandler(60, 60, 60));
    cp.addLast(handler);
  }

}
