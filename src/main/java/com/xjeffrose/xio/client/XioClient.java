package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.core.ShutdownUtil;
import io.airlift.units.Duration;
import java.io.Closeable;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientBossPool;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioWorkerPool;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.Timer;

import static com.google.common.base.Preconditions.checkNotNull;

public class XioClient implements Closeable {
  public static final Duration DEFAULT_CONNECT_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  private static final int DEFAULT_MAX_FRAME_SIZE = 16777216;

  private final XioClientConfig nettyClientConfig;
  private final ExecutorService bossExecutor;
  private final ExecutorService workerExecutor;
  private final NioClientSocketChannelFactory channelFactory;
  private final ChannelGroup allChannels = new DefaultChannelGroup();
  private final Timer timer;

  public XioClient() {
    this(XioClientConfig.newBuilder().build());
  }

  public XioClient(XioClientConfig xioClientConfig) {
    this.nettyClientConfig = xioClientConfig;

    this.timer = xioClientConfig.getTimer();
    this.bossExecutor = xioClientConfig.getBossExecutor();
    this.workerExecutor = xioClientConfig.getWorkerExecutor();

    int bossThreadCount = xioClientConfig.getBossThreadCount();
    int workerThreadCount = xioClientConfig.getWorkerThreadCount();

    NioWorkerPool workerPool = new NioWorkerPool(workerExecutor, workerThreadCount, ThreadNameDeterminer.CURRENT);
    NioClientBossPool bossPool = new NioClientBossPool(bossExecutor, bossThreadCount, timer, ThreadNameDeterminer.CURRENT);

    this.channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
  }

  public <T extends XioClientChannel> ListenableFuture<T> connect(URI uri) {

    return connectAsync(new XioClientConnector(uri),
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_MAX_FRAME_SIZE);
  }

  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(XioClientConnector clientChannelConnector,
      Duration connectTimeout,
      int maxFrameSize) {
    checkNotNull(clientChannelConnector, "clientChannelConnector is null");

    ClientBootstrap bootstrap = createClientBootstrap();
    bootstrap.setOptions(nettyClientConfig.getBootstrapOptions());

    if (connectTimeout != null) {
      bootstrap.setOption("connectTimeoutMillis", connectTimeout.toMillis());
    }

    bootstrap.setPipelineFactory(clientChannelConnector.newChannelPipelineFactory(maxFrameSize, nettyClientConfig));
    ChannelFuture nettyChannelFuture = clientChannelConnector.connect(bootstrap);

    nettyChannelFuture.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.getChannel();
        if (channel != null && channel.isOpen()) {
          allChannels.add(channel);
        }
      }
    });
    return new XioFuture<>(clientChannelConnector, nettyChannelFuture);
  }


  @Override
  public void close() {
    // Stop the timer thread first, so no timeouts can fire during the rest of the
    // shutdown process
    timer.stop();

    ShutdownUtil.shutdownChannelFactory(channelFactory,
        bossExecutor,
        workerExecutor,
        allChannels);
  }

  private ClientBootstrap createClientBootstrap() {
    return new ClientBootstrap(channelFactory);
  }

  private class XioFuture<T extends XioClientChannel> extends AbstractFuture<T> {
    private XioFuture(final XioClientConnector clientChannelConnector, final ChannelFuture channelFuture) {
      channelFuture.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          try {
            if (future.isSuccess()) {
              Channel channel = future.getChannel();
///////////////////////////////////////////////////////////////////
              ///////////////////////////////
            } else if (future.isCancelled()) {
              if (!cancel(true)) {

                setException(new XioClientConnectionException("Unable to cancel client channel connection"));
              }

            } else {
              throw future.getCause();
            }

          } catch (Throwable t) {
            setException(new XioClientConnectionException("Failed to connect client channel", t));
          }

        }
      });
    }
  }
}
