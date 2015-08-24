package com.xjeffrose.xio.client;


import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.core.ShutdownUtil;
import io.airlift.units.Duration;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
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
  public static final Duration DEFAULT_RECEIVE_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  public static final Duration DEFAULT_READ_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  private static final Duration DEFAULT_SEND_TIMEOUT = new Duration(2, TimeUnit.SECONDS);

  private static final int DEFAULT_MAX_FRAME_SIZE = 16777216;

  private final XioClientConfig xioClientConfig;
  private final ExecutorService bossExecutor;
  private final ExecutorService workerExecutor;
  private final NioClientSocketChannelFactory channelFactory;
  private final HostAndPort defaultSocksProxyAddress;
  private final ChannelGroup allChannels = new DefaultChannelGroup();
  private final Timer timer;

  /**
   * Creates a new XioClient with defaults: cachedThreadPool for bossExecutor and workerExecutor
   */
  public XioClient() {
    this(XioClientConfig.newBuilder().build());
  }

  public XioClient(XioClientConfig xioClientConfig) {
    this.xioClientConfig = xioClientConfig;

    this.timer = xioClientConfig.getTimer();
    this.bossExecutor = xioClientConfig.getBossExecutor();
    this.workerExecutor = xioClientConfig.getWorkerExecutor();
    this.defaultSocksProxyAddress = xioClientConfig.getDefaultSocksProxyAddress();

    int bossThreadCount = xioClientConfig.getBossThreadCount();
    int workerThreadCount = xioClientConfig.getWorkerThreadCount();

    NioWorkerPool workerPool = new NioWorkerPool(workerExecutor, workerThreadCount, ThreadNameDeterminer.CURRENT);
    NioClientBossPool bossPool = new NioClientBossPool(bossExecutor, bossThreadCount, timer, ThreadNameDeterminer.CURRENT);

    this.channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
  }

  private static InetSocketAddress toInetAddress(HostAndPort hostAndPort) {
    return (hostAndPort == null) ? null : new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
  }

  @SuppressWarnings("unchecked")
  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(XioClientConnector clientChannelConnector) {

    return connectAsync(clientChannelConnector,
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_RECEIVE_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        DEFAULT_SEND_TIMEOUT,
        DEFAULT_MAX_FRAME_SIZE,
        defaultSocksProxyAddress);
  }

  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(
      XioClientConnector<T> clientChannelConnector,
      @Nullable Duration connectTimeout,
      @Nullable Duration receiveTimeout,
      @Nullable Duration readTimeout,
      @Nullable Duration sendTimeout,
      int maxFrameSize) {

    return connectAsync(clientChannelConnector,
        connectTimeout,
        receiveTimeout,
        readTimeout,
        sendTimeout,
        maxFrameSize,
        defaultSocksProxyAddress);
  }

  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(
      XioClientConnector<T> clientChannelConnector,
      @Nullable Duration connectTimeout,
      @Nullable Duration receiveTimeout,
      @Nullable Duration readTimeout,
      @Nullable Duration sendTimeout,
      int maxFrameSize,
      @Nullable HostAndPort socksProxyAddress) {
    checkNotNull(clientChannelConnector, "clientChannelConnector is null");

    ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
    bootstrap.setOptions(xioClientConfig.getBootstrapOptions());

    if (connectTimeout != null) {
      bootstrap.setOption("connectTimeoutMillis", connectTimeout.toMillis());
    }

    bootstrap.setPipelineFactory(clientChannelConnector.newChannelPipelineFactory(maxFrameSize, xioClientConfig));
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
    return new XioFuture<>(clientChannelConnector,
        receiveTimeout,
        readTimeout,
        sendTimeout,
        nettyChannelFuture,
        xioClientConfig);
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
}
