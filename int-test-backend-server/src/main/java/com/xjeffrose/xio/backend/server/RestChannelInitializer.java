package com.xjeffrose.xio.backend.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestChannelInitializer extends ChannelInitializer<SocketChannel> {

  private final SslContext sslContext;
  private final RestHandlers appHandlers = new RestHandlers();
  private final boolean h2Capable;

  public RestChannelInitializer(SslContext sslContext, boolean h2Capable) {
    this.sslContext = sslContext;
    this.h2Capable = h2Capable;
  }

  @Override
  public void initChannel(SocketChannel ch) {
    log.debug("initialize tls:{} h2:{}", sslContext != null, h2Capable);
    if (sslContext != null && h2Capable) {
      configureTlsH2(ch);
    } else {
      ChannelPipeline pipeline = ch.pipeline();
      if (sslContext != null) {
        pipeline.addLast(sslContext.newHandler(ch.alloc()));
      }
      configureH1(ch.pipeline());
    }
  }

  private void configureTlsH2(SocketChannel ch) {
    ch.pipeline()
      .addLast(sslContext.newHandler(ch.alloc()), new Http2OrHttpHandler(this::configureH1, appHandlers.handlers()));
  }

  private void configureH1(ChannelPipeline pipeline) {
    pipeline
      .addLast("server codec duplex", new HttpServerCodec())
      .addLast("message size limit aggregator", new HttpObjectAggregator(512 * 1024))
      .addLast(new ChannelRequestInboundHandler(appHandlers.handlers()));
  }
}
