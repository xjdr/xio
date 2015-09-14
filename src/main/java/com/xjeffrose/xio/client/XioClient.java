package com.xjeffrose.xio.client;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.TimeUnit;

public class XioClient {
  private static final int NO_WRITER_IDLE_TIMEOUT = 0;
  private static final int NO_ALL_IDLE_TIMEOUT = 0;

  private final ChannelHandlerContext ctx;
  private final XioClientDef def;
  private ChannelInitializer<SocketChannel> pipelineFactory;
  private TimeUnit connectTimeout = null;

  public XioClient(XioClientDef def) {

    this(null, def);
  }

  public XioClient(ChannelHandlerContext ctx, XioClientDef def) {

    this.ctx = ctx;
    this.def = def;

//    this.channelStatistics = new ChannelStatistics(allChannels);

    //TODO: This is an ugly mess, clean this up
    this.pipelineFactory = new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline cp = channel.pipeline();
        XioSecurityHandlers securityHandlers = def.getSecurityFactory().getSecurityHandlers(def, xioServerConfig);
        cp.addLast("connectionContext", new ConnectionContextHandler());
        cp.addLast("connectionLimiter", connectionLimiter);
        cp.addLast(ChannelStatistics.NAME, channelStatistics);
        cp.addLast("encryptionHandler", securityHandlers.getEncryptionHandler());
        cp.addLast("codec", def.getCodecFactory().getCodec());
//        if (def.getClientIdleTimeout() != null) {
//          cp.addLast("idleTimeoutHandler", new IdleStateHandler(
//              def.getClientIdleTimeout().toMillis(),
//              NO_WRITER_IDLE_TIMEOUT,
//              NO_ALL_IDLE_TIMEOUT,
//              TimeUnit.MILLISECONDS));
//          cp.addLast("idleDisconnectHandler", new IdleDisconnectHandler(
//              (int) def.getClientIdleTimeout().toMillis(),
//              NO_WRITER_IDLE_TIMEOUT,
//              NO_ALL_IDLE_TIMEOUT));
//        }

        cp.addLast("authHandler", securityHandlers.getAuthenticationHandler());
//        cp.addLast("dispatcher", new XioDispatcher(def, xioServerConfig));
        cp.addLast("exceptionLogger", new XioExceptionLogger());
      }
    };
  }

  public void execute() {

  }

  private void connect() {
    Bootstrap bootstrap = new Bootstrap();

    if (ctx != null) {
      bootstrap
          .group(ctx.channel().eventLoop());
    } else {
      bootstrap
          .group(new NioEventLoopGroup());
    }

    bootstrap
        .channel(NioSocketChannel.class)
        .handler(pipelineFactory);

    if (connectTimeout != null) {
      bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
    }

    // Set some sane defaults
    bootstrap
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
        .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
        .option(ChannelOption.TCP_NODELAY, true);

    ChannelFuture nettyChannelFuture = bootstrap.connect(host, port);

    nettyChannelFuture.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
          // do something
        } else {
          // do something
        }
      }
    });
  }
}
