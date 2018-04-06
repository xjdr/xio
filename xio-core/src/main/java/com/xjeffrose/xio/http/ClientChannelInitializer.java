package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.pipeline.Pipelines;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import java.util.function.Supplier;
import lombok.val;

public class ClientChannelInitializer extends ChannelInitializer {

  private final ClientState state;
  private final Supplier<ChannelHandler> appHandler;
  private final XioTracing tracing;

  public ClientChannelInitializer(
      ClientState state, Supplier<ChannelHandler> appHandler, XioTracing tracing) {
    this.state = state;
    this.appHandler = appHandler;
    this.tracing = tracing;
  }

  private ChannelHandler buildHttp2Handler() {
    return new Http2HandlerBuilder().server(false).build();
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {
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
            new HttpClientNegotiationHandler(ClientChannelInitializer.this::buildHttp2Handler))
        .addLast("codec", CodecPlaceholderHandler.INSTANCE)
        .addLast("application codec", ApplicationCodecPlaceholderHandler.INSTANCE);
    if (tracing != null) {
      val traceHandler = tracing.newClientHandler(state.config.isTlsEnabled());
      Pipelines.addHandler(channel.pipeline(), "distributed tracing", traceHandler);
    }
    channel
        .pipeline()
        .addLast("idle handler", new XioIdleDisconnectHandler(60, 60, 60))
        .addLast("message logging", new XioMessageLogger(Client.class, "objects"))
        .addLast("request buffer", new RequestBuffer())
        .addLast("app handler", appHandler.get());
  }
}
