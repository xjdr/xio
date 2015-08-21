package com.xjeffrose.xio.core;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import org.jboss.netty.channel.socket.ServerSocketChannelConfig;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.jboss.netty.util.Timer;

import static java.util.concurrent.Executors.newCachedThreadPool;

public class NettyServerConfigBuilder extends NettyConfigBuilderBase<NettyServerConfigBuilder> {
  private final NioSocketChannelConfig socketChannelConfig = (NioSocketChannelConfig) Proxy.newProxyInstance(
      getClass().getClassLoader(),
      new Class<?>[]{NioSocketChannelConfig.class},
      new Magic("child.")
  );
  private final ServerSocketChannelConfig serverSocketChannelConfig = (ServerSocketChannelConfig) Proxy.newProxyInstance(
      getClass().getClassLoader(),
      new Class<?>[]{ServerSocketChannelConfig.class},
      new Magic(""));

  @Inject
  public NettyServerConfigBuilder() {
    getSocketChannelConfig().setTcpNoDelay(true);
  }

  public NioSocketChannelConfig getSocketChannelConfig() {
    return socketChannelConfig;
  }

  public ServerSocketChannelConfig getServerSocketChannelConfig() {
    return serverSocketChannelConfig;
  }

  public NettyServerConfig build() {
    Timer timer = getTimer();
    ExecutorService bossExecutor = getBossExecutor();
    int bossThreadCount = getBossThreadCount();
    ExecutorService workerExecutor = getWorkerExecutor();
    int workerThreadCount = getWorkerThreadCount();

    return new NettyServerConfig(
        getBootstrapOptions(),
        timer != null ? timer : new XioTimer(threadNamePattern("")),
        bossExecutor != null ? bossExecutor : buildDefaultBossExecutor(),
        bossThreadCount,
        workerExecutor != null ? workerExecutor : buildDefaultWorkerExecutor(),
        workerThreadCount
    );
  }

  private ExecutorService buildDefaultBossExecutor() {
    return newCachedThreadPool(renamingThreadFactory(threadNamePattern("-boss-%s")));
  }

  private ExecutorService buildDefaultWorkerExecutor() {
    return newCachedThreadPool(renamingThreadFactory(threadNamePattern("-worker-%s")));
  }

  private String threadNamePattern(String suffix) {
    String XioName = getXioName();
    return "Xio-server" + (Strings.isNullOrEmpty(XioName) ? "" : "-" + XioName) + suffix;
  }

  private ThreadFactory renamingThreadFactory(String nameFormat) {
    return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
  }
}