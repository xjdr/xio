package com.xjeffrose.xio.client;


import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.xjeffrose.xio.core.ShutdownUtil;
import com.xjeffrose.xio.core.XioException;
import io.airlift.units.Duration;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
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
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Timer;
import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
  private final EventLoopGroup group;
  private final HostAndPort defaultSocksProxyAddress;
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
    this.bossExecutor = xioClientConfig.getBossExecutor();
    this.workerExecutor = xioClientConfig.getWorkerExecutor();
    this.defaultSocksProxyAddress = xioClientConfig.getDefaultSocksProxyAddress();
    this.group = new NioEventLoopGroup(1);
    this.allChannels = new DefaultChannelGroup(this.group.next());
  }

  private static InetSocketAddress toInetAddress(HostAndPort hostAndPort) {
    return (hostAndPort == null) ? null : new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
  }

  public static DefaultFullHttpResponse call(URI uri) throws Exception {
    return call(null, uri, null);
  }

  public static DefaultFullHttpResponse call(URI uri, ByteBuf req) throws Exception {
    return call(null, uri, req);
  }

  public static DefaultFullHttpResponse call(@Nullable XioClientConfig config, URI uri) throws Exception {
    return call(config, uri, null);
  }

  private static void onError(XioException e) throws XioException {
    throw e;
  }

  public static DefaultFullHttpResponse call(@Nullable XioClientConfig config, URI uri, @Nullable ByteBuf req) throws Exception {
    final Lock lock = new ReentrantLock();
    final Condition waitForFinish = lock.newCondition();
    final XioClient xioClient;
    if (config != null) {
      final XioClientConfig xioClientConfig = config;
      xioClient = new XioClient(xioClientConfig);
    } else {
      xioClient = new XioClient();
    }

    ListenableFuture<XioClientChannel> responseFuture = null;

    responseFuture = xioClient.connectAsync(new HttpClientConnector(uri));

    XioClientChannel xioClientChannel = null;

    if (!responseFuture.isCancelled()) {
      xioClientChannel = responseFuture.get((long) 2000, TimeUnit.MILLISECONDS);
    }

    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

    //TODO(JR): Make xio version pull from config
    httpClientChannel.setHeaders(ImmutableMap.of(
        HttpHeaders.HOST, uri.getHost(),
        HttpHeaders.USER_AGENT, "xio"));

    Listener listener = new Listener() {
      ByteBuf response;

      @Override
      public void onRequestSent() {
//                        System.out.println("Request Sent");
      }

      @Override
      public void onResponseReceived(ByteBuf message) {
        response = message;
        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public void onChannelError(XioException requestException) {
        StringBuilder sb = new StringBuilder();
        sb.append(HttpVersion.HTTP_1_1)
            .append(" ")
            .append(HttpResponseStatus.INTERNAL_SERVER_ERROR)
            .append("\r\n")
            .append("\r\n\r\n")
            .append(requestException.getMessage())
            .append("\n");

        response = Unpooled.wrappedBuffer(sb.toString().getBytes());

        lock.lock();
        waitForFinish.signalAll();
        lock.unlock();
      }

      @Override
      public ByteBuf getResponse() {
        return response;
      }

    };

    httpClientChannel.sendAsynchronousRequest(req, false, listener);

    lock.lock();
    waitForFinish.await();
    lock.unlock();


    // Lets make a HTTP parser cause apparently that's a good idea...
    ByteBuf response = listener.getResponse();
    String[] headerBody = response.toString(Charset.defaultCharset()).split("\r\n\r\n");
    String[] headers = headerBody[0].split("\r\n");
    String[] firstLine = headers[0].split("\\s");

    // Lets make a HTTP Response object now
    DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
        HttpVersion.valueOf(firstLine[0]),
        new HttpResponseStatus(Integer.parseInt(firstLine[1]), firstLine[2]),
        Unpooled.wrappedBuffer(headerBody[1].getBytes()));

    for (int i = 1; i < headers.length; i++) {
      String[] xs = headers[i].split(":");
      httpResponse.headers().add(xs[0].trim(), xs[1].trim());
    }

    return httpResponse;
  }

  @SuppressWarnings("unchecked")
  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(XioClientConnector clientChannelConnector) {

    return connectAsync(//null,
        clientChannelConnector,
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_RECEIVE_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        DEFAULT_SEND_TIMEOUT,
        DEFAULT_MAX_FRAME_SIZE,
        defaultSocksProxyAddress);
  }
//
//  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(ChannelHandlerContext ctx, XioClientConnector clientChannelConnector) {
//
//    return connectAsync(//ctx,
//        clientChannelConnector,
//        DEFAULT_CONNECT_TIMEOUT,
//        DEFAULT_RECEIVE_TIMEOUT,
//        DEFAULT_READ_TIMEOUT,
//        DEFAULT_SEND_TIMEOUT,
//        DEFAULT_MAX_FRAME_SIZE,
//        defaultSocksProxyAddress);
//  }

  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(
      //ChannelHandlerContext ctx,
      XioClientConnector clientChannelConnector,
      @Nullable Duration connectTimeout,
      @Nullable Duration receiveTimeout,
      @Nullable Duration readTimeout,
      @Nullable Duration sendTimeout,
      int maxFrameSize) {

    return connectAsync(//ctx,
        clientChannelConnector,
        connectTimeout,
        receiveTimeout,
        readTimeout,
        sendTimeout,
        maxFrameSize,
        defaultSocksProxyAddress);
  }

  public <T extends XioClientChannel> ListenableFuture<T> connectAsync(
      //ChannelHandlerContext ctx,
      XioClientConnector clientChannelConnector,
      @Nullable Duration connectTimeout,
      @Nullable Duration receiveTimeout,
      @Nullable Duration readTimeout,
      @Nullable Duration sendTimeout,
      int maxFrameSize,
      @Nullable HostAndPort socksProxyAddress) {
    checkNotNull(clientChannelConnector, "clientChannelConnector is null");

    Bootstrap bootstrap = new Bootstrap();

//    if (ctx != null) {
//      bootstrap.group(ctx.channel().eventLoop());
//    } else {
    bootstrap.group(group);
//    }

    bootstrap
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

    ShutdownUtil.shutdownChannelFactory(
        group,
        null,
        null,
        allChannels);
  }
}
