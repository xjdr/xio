package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.pipeline.Pipelines;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpServerCodec;
import java.util.function.Supplier;
import lombok.Setter;
import lombok.val;

public class ClientChannelInitializer extends ChannelInitializer {

  public static final String APP_HANDLER = "app handler";

  private final ClientState state;
  @Setter private Supplier<ChannelHandler> appHandler;
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
      // This client has SSL configured, this allows us to do several things dynamically
      channel
          .pipeline()
          .addLast(
              "ssl handler",
              state.sslContext.newHandler(
                  channel.alloc(), state.remote.getHostString(), state.remote.getPort()))
          // SSL allows us to use ALPN to negotiate for either http1 or http2
          .addLast(
              "negotiation handler",
              new HttpClientNegotiationHandler(ClientChannelInitializer.this::buildHttp2Handler))
          // ALPN will allow us to swap this out for the appropriate netty codec
          .addLast("codec", CodecPlaceholderHandler.INSTANCE)
          // ALPN will allow us to swap this out for the appropriate xio codec
          .addLast("application codec", ApplicationCodecPlaceholderHandler.INSTANCE);
    } else {
      // This client does not have SSL configured so we can make a few assumptions
      // No need for a negotiation handler as we have no ALPN
      // No need for an http2 handler as we don't allow that over cleartext
      channel
          .pipeline()
          .addLast("codec", new HttpServerCodec())
          .addLast("application codec", new Http1ServerCodec());
    }
    if (tracing != null) {
      val traceHandler = tracing.newClientHandler(state.config.isTlsEnabled());
      Pipelines.addHandler(channel.pipeline(), "distributed tracing", traceHandler);
    }
    channel
        .pipeline()
        .addLast("idle handler", new XioIdleDisconnectHandler(60, 60, 60))
        .addLast("message logging", new XioMessageLogger(Client.class, "objects"))
        .addLast("request buffer", new RequestBuffer())
        .addLast(APP_HANDLER, appHandler.get());
  }
}
