package com.xjeffrose.xio.server;

import com.google.common.base.Preconditions;
import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.ShutdownUtil;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.core.XioMetrics;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

public class XioServer {
  private static final Logger log = Logger.getLogger(XioServerTransport.class.getName());

  private static final int NO_WRITER_IDLE_TIMEOUT = 60000;
  private static final int NO_ALL_IDLE_TIMEOUT = 60000;
  private static final XioConnectionLimiter globalConnectionLimiter = new XioConnectionLimiter(15000);

  private final int requestedPort;
  private final InetSocketAddress hostAddr;
  private final ChannelGroup allChannels;
  private final XioServerDef def;
  private final XioServerConfig xioServerConfig;
  private final XioService xioService;
  private final ChannelStatistics channelStatistics;
  private final ChannelInitializer<SocketChannel> pipelineFactory;
  private int actualPort;
  private ServerBootstrap bootstrap;
  private ExecutorService bossExecutor;
  private ExecutorService ioWorkerExecutor;
  private Channel serverChannel;

  public XioServer(final XioServerDef def, final XioService xioService) {
    this(def, XioServerConfig.newBuilder().build(), xioService, new DefaultChannelGroup(new NioEventLoopGroup().next()));
  }

  public XioServer(
    final XioServerDef def,
    final XioServerConfig xioServerConfig,
    final XioService xioService,
    final ChannelGroup allChannels) {
    this.def = def;
    this.xioServerConfig = xioServerConfig;
    this.xioService = xioService;
    this.requestedPort = def.getServerPort();
    this.hostAddr = def.getHostAddress();
    this.allChannels = allChannels;
    this.channelStatistics = new ChannelStatistics(allChannels);

    //TODO(JR): This is an ugly mess, clean this up
    this.pipelineFactory = new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline cp = channel.pipeline();
        XioSecurityHandlers securityHandlers = def.getSecurityFactory().getSecurityHandlers(def, xioServerConfig);
        cp.addLast("connectionContext", new ConnectionContextHandler());
        cp.addLast("globalConnectionLimiter", globalConnectionLimiter);
        cp.addLast("serviceConnectionLimiter", new XioConnectionLimiter(def.getMaxConnections()));
        cp.addLast(ChannelStatistics.NAME, channelStatistics);
        cp.addLast("encryptionHandler", securityHandlers.getEncryptionHandler());
        cp.addLast("messageLogger", new XioMessageLogger());
        cp.addLast("codec", def.getCodecFactory().getCodec());
        //cp.addLast("aggregator", def.getAggregatorFactory().getAggregator());
        //cp.addLast("routingFilter", def.getRoutingFilterFactory().getRoutingFilter());
        if (def.getClientIdleTimeout() != null) {
          cp.addLast("idleDisconnectHandler", new XioIdleDisconnectHandler(
              (int) def.getClientIdleTimeout().toMillis(),
              NO_WRITER_IDLE_TIMEOUT,
              NO_ALL_IDLE_TIMEOUT,
              TimeUnit.MILLISECONDS));
        }
        cp.addLast("authHandler", securityHandlers.getAuthenticationHandler());
        // cp.addLast("xioServiceManager", new XioServiceManager(xioService));
        cp.addLast("xioService", new XioService());
        cp.addLast("exceptionLogger", new XioExceptionLogger());
      }
    };
  }

  public void serve() {
    start();
  }

  public void start() {
    bossExecutor = xioServerConfig.getBossExecutor();
    int bossThreadCount = xioServerConfig.getBossThreadCount();
    ioWorkerExecutor = xioServerConfig.getWorkerExecutor();
    int ioWorkerThreadCount = xioServerConfig.getWorkerThreadCount();

    if (Epoll.isAvailable()) {
      start(new EpollEventLoopGroup(bossThreadCount), new EpollEventLoopGroup(ioWorkerThreadCount));
    } else {
      start(new NioEventLoopGroup(bossThreadCount), new NioEventLoopGroup(ioWorkerThreadCount));
    }
  }

  private void start(NioEventLoopGroup bossGroup, NioEventLoopGroup workerGroup) {
    bootstrap = new ServerBootstrap();
    bootstrap
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(pipelineFactory);

    xioServerConfig.getBootstrapOptions().entrySet().forEach(xs -> {
      bootstrap.option(xs.getKey(), xs.getValue());
    });

    //Set some sane defaults
    bootstrap
      .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true))
      .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
      .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
      .option(ChannelOption.SO_BACKLOG, 128)
      .option(ChannelOption.TCP_NODELAY, true);

    try {
      serverChannel = bootstrap.bind(hostAddr).sync().channel();
    } catch (Throwable e) {
      //TODO(JR): Do somefin here
//      e.printStackTrace();
      String msg = e.getMessage() + " (" + hostAddr + ")";
      log.error(msg, e);
      throw new RuntimeException(msg, e);
    }
    InetSocketAddress actualSocket = (InetSocketAddress) serverChannel.localAddress();
    actualPort = actualSocket.getPort();
    Preconditions.checkState(actualPort != 0 && (actualPort == requestedPort || requestedPort == 0));
    log.info("started transport " + def.getName() + ":" + actualPort);
  }

  private void start(EpollEventLoopGroup bossGroup, EpollEventLoopGroup workerGroup) {
    bootstrap = new ServerBootstrap();
    bootstrap
        .group(bossGroup, workerGroup)
        .channel(EpollServerSocketChannel.class)
        .childHandler(pipelineFactory);

    xioServerConfig.getBootstrapOptions().entrySet().forEach(xs -> {
      bootstrap.option(xs.getKey(), xs.getValue());
    });

    //Set some sane defaults
    bootstrap
      .option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true))
      .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
      .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.SO_REUSEADDR, true);

    try {
      serverChannel = bootstrap.bind(hostAddr).sync().channel();
    } catch (Throwable e) {
      //TODO(JR): Do somefin here
//      e.printStackTrace();
      String msg = e.getMessage() + " (" + hostAddr + ")";
      log.error(msg, e);
      throw new RuntimeException(msg, e);
    }
    InetSocketAddress actualSocket = (InetSocketAddress) serverChannel.localAddress();
    actualPort = actualSocket.getPort();
    Preconditions.checkState(actualPort != 0 && (actualPort == requestedPort || requestedPort == 0));
    log.info("started transport " + def.getName() + ":" + actualPort);
  }

  public void stop()
      throws InterruptedException {
    if (serverChannel != null) {
//      log.info("stopping transport %s:%s", def.getName(), actualPort);
//       first stop accepting
      final CountDownLatch latch = new CountDownLatch(1);
      serverChannel.close().addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future)
            throws Exception {
          // stop and process remaining in-flight invocations
          if (def.getExecutor() instanceof ExecutorService) {
            ExecutorService exe = (ExecutorService) def.getExecutor();
            ShutdownUtil.shutdownExecutor(exe, "dispatcher");
          }
          latch.countDown();
        }
      });
      latch.await();
      serverChannel = null;
      log.info("stopped transport " + def.getName() + ":" + actualPort);
    }
  }

  public Channel getServerChannel() {
    return serverChannel;
  }

  public int getPort() {
    if (actualPort != 0) {
      return actualPort;
    } else {
      return requestedPort; // may be 0 if server not yet started
    }
  }

  public XioMetrics getMetrics() {
    return channelStatistics;
  }
}
