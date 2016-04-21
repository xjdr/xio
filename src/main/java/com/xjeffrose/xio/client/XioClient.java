package com.xjeffrose.xio.client;


import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.client.retry.RetryPolicy;
import com.xjeffrose.xio.client.retry.RetrySleeper;
import com.xjeffrose.xio.core.ShutdownUtil;
import io.airlift.units.Duration;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Timer;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unchecked")
public class XioClient implements Closeable {
  private static final Logger log = Logger.getLogger(XioClient.class);

  public static final Duration DEFAULT_CONNECT_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  public static final Duration DEFAULT_RECEIVE_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  public static final Duration DEFAULT_READ_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  private static final Duration DEFAULT_SEND_TIMEOUT = new Duration(2, TimeUnit.SECONDS);
  private static final int DEFAULT_MAX_FRAME_SIZE = 16777216;

  private final XioClientConfig xioClientConfig;
  private final EventLoopGroup group;
  private final ChannelGroup allChannels;
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
    this.group = new NioEventLoopGroup(1);
    this.allChannels = new DefaultChannelGroup(this.group.next());
  }

  private static InetSocketAddress toInetAddress(HostAndPort hostAndPort) {
    return (hostAndPort == null) ? null : new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
  }

  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(XioClientConnector clientChannelConnector, RetryPolicy retryPolicy) {

    return connectAsync(
        null,
        clientChannelConnector,
        retryPolicy,
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_RECEIVE_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        DEFAULT_SEND_TIMEOUT,
        DEFAULT_MAX_FRAME_SIZE);
  }

  @SuppressWarnings("unchecked")
  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(@Nullable ChannelHandlerContext ctx, XioClientConnector clientChannelConnector,  RetryPolicy retryPolicy) {

    return connectAsync(
        ctx,
        clientChannelConnector,
        retryPolicy,
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_RECEIVE_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        DEFAULT_SEND_TIMEOUT,
        DEFAULT_MAX_FRAME_SIZE);
  }

//  @SuppressWarnings("unchecked")
  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(
      ChannelHandlerContext ctx,
      XioClientConnector clientChannelConnector,
      RetryPolicy retryPolicy,
      @Nullable Duration connectTimeout,
      @Nullable Duration receiveTimeout,
      @Nullable Duration readTimeout,
      @Nullable Duration sendTimeout,
      int maxFrameSize) {
    checkNotNull(clientChannelConnector, "clientChannelConnector is null");

    int retryCount = 0;
    long elapsedTime = 0;
    final Bootstrap bootstrap = new Bootstrap();
    final RetrySleeper sleeper = (time, unit) -> {
      Thread.sleep(time);
    };

    if (ctx != null) {
      bootstrap.group(ctx.channel().eventLoop().parent());
    } else {
      bootstrap.group(group);
    }

    bootstrap
        .channel(NioSocketChannel.class)
        .handler(clientChannelConnector.newChannelPipelineFactory(maxFrameSize, xioClientConfig));

    if (connectTimeout != null) {
      bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
    }

    // Set some sane defaults
    bootstrap
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 32 * 1024)
        .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 8 * 1024)
        .option(ChannelOption.TCP_NODELAY, true);

    xioClientConfig.getBootstrapOptions().entrySet().forEach(xs -> {
      bootstrap.option(xs.getKey(), xs.getValue());
    });

    ChannelFuture nettyChannelFuture = connect(clientChannelConnector, bootstrap, retryPolicy, retryCount, sleeper);

    return new XioFuture<>(clientChannelConnector,
        receiveTimeout,
        readTimeout,
        sendTimeout,
        nettyChannelFuture,
        xioClientConfig);
  }

  private static long getElapsedTime(long startTime) {
    return startTime - System.currentTimeMillis();
  }

  private ChannelFuture connect(XioClientConnector clientChannelConnector, Bootstrap bootstrap, RetryPolicy retryPolicy, int retryCount, RetrySleeper sleeper) {
    final long connectStart = System.currentTimeMillis();
    ChannelFuture[] channelFuture = new ChannelFuture[1];
    channelFuture[0] = clientChannelConnector.connect(bootstrap);
    channelFuture[0].addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.channel();
        if (channel != null && channel.isOpen()) {
          allChannels.add(channel);
        } else {
          if (retryPolicy.allowRetry(retryCount, getElapsedTime(connectStart), sleeper)) {
            int newCount = retryCount + 1;
            channelFuture[0] = connect(clientChannelConnector, bootstrap, retryPolicy, newCount, sleeper);
          } else {
            log.error("Retry Count Exceeded - Failed to connect to server");
          }
        }
      }
    });
    return channelFuture[0];
  }

  @Override
  public void close() {
    // Stop the timer thread first, so no timeouts can fire during the rest of the
    // shutdown process
    timer.stop();

    ShutdownUtil.shutdownChannelFactory(
        group,
        null,
        null,
        allChannels);
  }
}
