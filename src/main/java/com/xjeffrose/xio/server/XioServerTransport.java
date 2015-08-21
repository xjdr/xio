package com.xjeffrose.xio.server;

import com.google.common.base.Preconditions;
import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.ShutdownUtil;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioMetrics;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import io.airlift.log.Logger;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerBossPool;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.handler.codec.http.HttpServerCodec;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.ThreadNameDeterminer;


public class XioServerTransport implements ExternalResourceReleasable {
  private static final Logger log = Logger.get(XioServerTransport.class);
  private static final int NO_WRITER_IDLE_TIMEOUT = 0;
  private static final int NO_ALL_IDLE_TIMEOUT = 0;
  private final int requestedPort;
  private final InetSocketAddress hostAddr;
  private final ChannelPipelineFactory pipelineFactory;
  private final ChannelGroup allChannels;
  private final XioServerDef def;
  private final XioServerConfig xioServerConfig;
  private final ChannelStatistics channelStatistics;
  private int actualPort;
  private ServerBootstrap bootstrap;
  private ExecutorService bossExecutor;
  private ExecutorService ioWorkerExecutor;
  private ServerChannelFactory channelFactory;
  private Channel serverChannel;

  public XioServerTransport(final XioServerDef def) {
    this(def, XioServerConfig.newBuilder().build(), new DefaultChannelGroup());
  }

  @Inject
  public XioServerTransport(
      final XioServerDef def,
      final XioServerConfig xioServerConfig,
      final ChannelGroup allChannels) {
    this.def = def;
    this.xioServerConfig = xioServerConfig;
    this.requestedPort = def.getServerPort();
    this.hostAddr = def.getHostAddress();
    this.allChannels = allChannels;
    // connectionLimiter must be instantiated exactly once (and thus outside the pipeline factory)
    final ConnectionLimiter connectionLimiter = new ConnectionLimiter(def.getMaxConnections());

    this.channelStatistics = new ChannelStatistics(allChannels);

    //TODO: This is an ugly mess, clean this up
    this.pipelineFactory = new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline()
          throws Exception {
        ChannelPipeline cp = Channels.pipeline();
        XioSecurityHandlers securityHandlers = def.getSecurityFactory().getSecurityHandlers(def, xioServerConfig);
        cp.addLast("connectionContext", new ConnectionContextHandler());
        cp.addLast("connectionLimiter", connectionLimiter);
        cp.addLast(ChannelStatistics.NAME, channelStatistics);
        cp.addLast("encryptionHandler", securityHandlers.getEncryptionHandler());
        cp.addLast("httpCodec", new HttpServerCodec());
        if (def.getClientIdleTimeout() != null) {
          cp.addLast("idleTimeoutHandler", new IdleStateHandler(xioServerConfig.getTimer(),
              def.getClientIdleTimeout().toMillis(),
              NO_WRITER_IDLE_TIMEOUT,
              NO_ALL_IDLE_TIMEOUT,
              TimeUnit.MILLISECONDS));
          cp.addLast("idleDisconnectHandler", new IdleDisconnectHandler());
        }

        cp.addLast("authHandler", securityHandlers.getAuthenticationHandler());
        cp.addLast("dispatcher", new XioDispatcher(def, xioServerConfig.getTimer()));
        cp.addLast("exceptionLogger", new XioExceptionLogger());
        return cp;
      }
    };
  }

  public void start() {
    bossExecutor = xioServerConfig.getBossExecutor();
    int bossThreadCount = xioServerConfig.getBossThreadCount();
    ioWorkerExecutor = xioServerConfig.getWorkerExecutor();
    int ioWorkerThreadCount = xioServerConfig.getWorkerThreadCount();

    channelFactory = new NioServerSocketChannelFactory(new NioServerBossPool(bossExecutor, bossThreadCount, ThreadNameDeterminer.CURRENT),
        new NioWorkerPool(ioWorkerExecutor, ioWorkerThreadCount, ThreadNameDeterminer.CURRENT));
    start(channelFactory);
  }

  public void start(ServerChannelFactory serverChannelFactory) {
    bootstrap = new ServerBootstrap(serverChannelFactory);
    bootstrap.setOptions(xioServerConfig.getBootstrapOptions());
    bootstrap.setPipelineFactory(pipelineFactory);
    serverChannel = bootstrap.bind(hostAddr);
    InetSocketAddress actualSocket = (InetSocketAddress) serverChannel.getLocalAddress();
    actualPort = actualSocket.getPort();
    log.info(""+actualPort);
    log.info(""+requestedPort);
    Preconditions.checkState(actualPort != 0 && (actualPort == requestedPort || requestedPort == 0));
    log.info("started transport %s:%s", def.getName(), actualPort);
  }

  public void stop()
      throws InterruptedException {
    if (serverChannel != null) {
      log.info("stopping transport %s:%s", def.getName(), actualPort);
      // first stop accepting
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
    }

    // If the channelFactory was created by us, we should also clean it up. If the
    // channelFactory was passed in by XioBootstrap, then it may be shared so don't clean
    // it up.
    if (channelFactory != null) {
      ShutdownUtil.shutdownChannelFactory(channelFactory,
          bossExecutor,
          ioWorkerExecutor,
          allChannels);
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

  @Override
  public void releaseExternalResources() {
    bootstrap.releaseExternalResources();
  }

  public XioMetrics getMetrics() {
    return channelStatistics;
  }

  private static class ConnectionLimiter extends SimpleChannelUpstreamHandler {
    private final AtomicInteger numConnections;
    private final int maxConnections;

    public ConnectionLimiter(int maxConnections) {
      this.maxConnections = maxConnections;
      this.numConnections = new AtomicInteger(0);
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
      if (maxConnections > 0) {
        if (numConnections.incrementAndGet() > maxConnections) {
          ctx.getChannel().close();
          // numConnections will be decremented in channelClosed
          log.info("Accepted connection above limit (%s). Dropping.", maxConnections);
        }
      }
      super.channelOpen(ctx, e);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
      if (maxConnections > 0) {
        if (numConnections.decrementAndGet() < 0) {
          log.error("BUG in ConnectionLimiter");
        }
      }
      super.channelClosed(ctx, e);
    }
  }
}
