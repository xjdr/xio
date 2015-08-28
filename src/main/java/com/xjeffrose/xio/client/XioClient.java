package com.xjeffrose.xio.client;


import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.core.ShutdownUtil;
import io.airlift.units.Duration;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Timer;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;


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
//  private final NioClientSocketChannelFactory channelFactory;
  private final EventLoopGroup group;
  private final HostAndPort defaultSocksProxyAddress;
  private final ChannelGroup allChannels;
  private final Timer timer;
  private final EventLoopGroup bossGroup;
  private final EventLoopGroup workerGroup;

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
    this.bossGroup = new NioEventLoopGroup(bossThreadCount);
    this.workerGroup = new NioEventLoopGroup(workerThreadCount);
    this.group = new NioEventLoopGroup();
    this.allChannels = new DefaultChannelGroup(this.group.next());
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

    Bootstrap bootstrap = new Bootstrap();
    bootstrap
        .group(new NioEventLoopGroup())
        .channel(NioSocketChannel.class)
        .handler(clientChannelConnector.newChannelPipelineFactory(maxFrameSize, xioClientConfig));

    xioClientConfig.getBootstrapOptions().entrySet().forEach(xs -> {
      bootstrap.option(xs.getKey(), xs.getValue());
    });

    if (connectTimeout != null) {
      bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
    }

    // Set some sane defaults
    bootstrap
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
        .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
        .option(ChannelOption.TCP_NODELAY, true);

    ChannelFuture nettyChannelFuture = clientChannelConnector.connect(bootstrap);
    nettyChannelFuture.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.channel();
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

    ShutdownUtil.shutdownChannelFactory(group,
        bossExecutor,
        workerExecutor,
        allChannels);
  }
}
