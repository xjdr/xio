package com.xjeffrose.xio.client;

import com.xjeffrose.xio.SSL.XioSecurityHandlerImpl;
import com.xjeffrose.xio.client.loadbalancer.Node;
import com.xjeffrose.xio.client.loadbalancer.Protocol;
import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import org.apache.log4j.Logger;

public class XioClientBootstrap {
  private static final Logger log = Logger.getLogger(Node.class);
  private final Bootstrap bootstrap;
  private boolean ssl;
  private Protocol proto;

  public XioClientBootstrap(ChannelHandler handler, int workerPoolSize, boolean ssl, Protocol proto) {
    if (Epoll.isAvailable()) {
      this.bootstrap = buildBootstrap(handler, new EpollEventLoopGroup(workerPoolSize));
    } else {
      this.bootstrap = buildBootstrap(handler, new NioEventLoopGroup(workerPoolSize));
    }
  }

  public Bootstrap buildBootstrap(ChannelHandler handler, EpollEventLoopGroup group) {

    if (Epoll.isAvailable()) {
      ChannelInitializer<EpollSocketChannel> pipeline = new ChannelInitializer<EpollSocketChannel>() {
        @Override
        protected void initChannel(EpollSocketChannel channel) throws Exception {
          ChannelPipeline cp = channel.pipeline();
          if (ssl) {
            cp.addLast("encryptionHandler", new XioSecurityHandlerImpl(true).getEncryptionHandler());
          }
          if (proto == (Protocol.HTTP)) {
            cp.addLast(new HttpClientCodec());
          }
          if (proto == (Protocol.HTTPS)) {
            cp.addLast(new HttpClientCodec());
          }
          cp.addLast(new XioIdleDisconnectHandler(60, 60, 60));
          cp.addLast(handler);
        }
      };

      return new Bootstrap()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
        .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
        .option(ChannelOption.TCP_NODELAY, true)
        .group(group)
        .channel(NioSocketChannel.class)
        .handler(pipeline);
    }

    return null;
  }

  public Bootstrap buildBootstrap(ChannelHandler handler, NioEventLoopGroup group) {
    ChannelInitializer<NioSocketChannel> pipeline = new ChannelInitializer<NioSocketChannel>() {
      @Override
      protected void initChannel(NioSocketChannel channel) throws Exception {
        ChannelPipeline cp = channel.pipeline();
        if (ssl) {
          cp.addLast("encryptionHandler", new XioSecurityHandlerImpl(true).getEncryptionHandler());
        }
        if (proto == (Protocol.HTTP)) {
          cp.addLast(new HttpClientCodec());
        }
        if (proto == (Protocol.HTTPS)) {
          cp.addLast(new HttpClientCodec());
        }
        cp.addLast(new XioIdleDisconnectHandler(60, 60, 60));
        cp.addLast(handler);
      }
    };

    return new Bootstrap()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
      .option(ChannelOption.SO_REUSEADDR, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
      .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
      .option(ChannelOption.TCP_NODELAY, true)
      .group(group)
      .channel(NioSocketChannel.class)
      .handler(pipeline);
  }

  public XioClient buildClient(String host, int port) {
    return new XioClient(host, port, bootstrap, ssl);
  }

  public Bootstrap getBootstrap() {
    return bootstrap;
  }
}

