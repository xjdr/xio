package com.xjeffrose.xio.backend.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

@Slf4j
class RestServer {
  private final int port;
  private final boolean useTls;
  private static final int NUM_THREADS = 10;
  private final boolean h2Capable;

  RestServer(int port, boolean useTls, boolean h2Capable) {
    this.port = port;
    this.useTls = useTls;
    this.h2Capable = h2Capable;
  }

  private SslContext sslContext() throws CertificateException, SSLException {
    if (useTls) {
      SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
      SelfSignedCertificate ssc = new SelfSignedCertificate();
      final String[] protocolNames;
      if (h2Capable) {
        protocolNames = new String[]{ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1};
      } else {
        protocolNames = new String[]{ApplicationProtocolNames.HTTP_1_1};
      }
      return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
        .sslProvider(provider)
        /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
         * Please refer to the HTTP/2 specification for cipher requirements. */
        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
        .applicationProtocolConfig(new ApplicationProtocolConfig(
          ApplicationProtocolConfig.Protocol.ALPN,
          // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
          // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
          protocolNames))
        .build();
    } else {
      return null;
    }
  }

  void start() throws InterruptedException, CertificateException, SSLException {
    ChannelConfig config = new ChannelConfig();
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(config.eventLoopGroup)
        .channel(config.channelClass)
        .localAddress(new InetSocketAddress(port))
        .childHandler(new RestChannelInitializer(sslContext(), h2Capable));
      ChannelFuture channelFuture = bootstrap.bind().sync();
      log.warn("starting to accept connections");
      channelFuture.channel().closeFuture().sync();
    } finally {
      config.eventLoopGroup.shutdownGracefully().sync();
    }
  }

  private static class ChannelConfig {
    final EventLoopGroup eventLoopGroup;
    final Class<? extends ServerChannel> channelClass;

    ChannelConfig() {
      if (Epoll.isAvailable()) {
        eventLoopGroup = new EpollEventLoopGroup(NUM_THREADS);
        channelClass = EpollServerSocketChannel.class;
      } else {
        eventLoopGroup = new NioEventLoopGroup(NUM_THREADS);
        channelClass = NioServerSocketChannel.class;
      }
    }
  }
}
