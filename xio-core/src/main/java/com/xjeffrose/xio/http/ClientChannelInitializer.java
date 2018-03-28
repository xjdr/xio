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

public class ClientChannelInitializer {
  public static final String sslHandlerName = "ssl handler";
  public static final String negotiationHandlerName = "negotiation handler";
  public static final String codecName = "codec";
  public static final String tracingName = "distributed tracing";
  public static final String applicationCodecName = "application codec";
  public static final String idleHandlerName = "idle handler";
  public static final String messageLoggingName = "message logging";
  public static final String requestBufferName = "request buffer";
  public static final String appHandlerName = "app handler";

  private final ClientState state;
  private final Supplier<ChannelHandler> appHandler;
  private final XioTracing tracing;

  public ClientChannelInitializer(
      ClientState state, Supplier<ChannelHandler> appHandler, XioTracing tracing) {
    this.state = state;
    this.appHandler = appHandler;
    this.tracing = tracing;
  }

  public ChannelInitializer createChannelInitializer() {
    return new ChannelInitializer() {
      public void initChannel(Channel channel) {
        if (state.sslContext != null) {
          channel
              .pipeline()
              .addLast(
                  sslHandlerName,
                  state.sslContext.newHandler(
                      channel.alloc(), state.remote.getHostString(), state.remote.getPort()));
        }
        channel
            .pipeline()
            .addLast(
                negotiationHandlerName,
                new HttpClientNegotiationHandler(ClientChannelInitializer.this::buildHttp2Handler))
            .addLast(codecName, CodecPlaceholderHandler.INSTANCE);
        if (tracing != null) {
          val traceHandler = tracing.newClientHandler(state.config.isTlsEnabled());
          Pipelines.addHandler(channel.pipeline(), tracingName, traceHandler);
        }
        channel
            .pipeline()
            .addLast(applicationCodecName, ApplicationCodecPlaceholderHandler.INSTANCE)
            .addLast(idleHandlerName, new XioIdleDisconnectHandler(60, 60, 60))
            .addLast(messageLoggingName, new XioMessageLogger(Client.class, "objects"))
            .addLast(requestBufferName, new RequestBuffer())
            .addLast(appHandlerName, appHandler.get());
      }
    };
  }

  private ChannelHandler buildHttp2Handler() {
    return new Http2HandlerBuilder().server(false).build();
  }
}
