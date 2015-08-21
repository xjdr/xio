package com.xjeffrose.xio.client;

import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.xjeffrose.xio.core.XioConfigBuilderBase;
import com.xjeffrose.xio.core.XioTimer;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.jboss.netty.util.Timer;

import static java.util.concurrent.Executors.newCachedThreadPool;


public class XioClientConfigBuilder extends XioConfigBuilderBase<XioClientConfigBuilder> {
  private final NioSocketChannelConfig socketChannelConfig = (NioSocketChannelConfig) Proxy.newProxyInstance(
      getClass().getClassLoader(),
      new Class<?>[]{NioSocketChannelConfig.class},
      new Magic("")
  );
  private HostAndPort defaultSocksProxyAddress = null;

  @Inject
  public XioClientConfigBuilder() {
    getSocketChannelConfig().setTcpNoDelay(true);
  }

  public NioSocketChannelConfig getSocketChannelConfig() {
    return socketChannelConfig;
  }

  public XioClientConfig build() {
    Timer timer = getTimer();
    ExecutorService bossExecutor = getBossExecutor();
    int bossThreadCount = getBossThreadCount();
    ExecutorService workerExecutor = getWorkerExecutor();
    int workerThreadCount = getWorkerThreadCount();

    return new XioClientConfig(
        getBootstrapOptions(),
        defaultSocksProxyAddress,
        timer != null ? timer : new XioTimer(threadNamePattern("")),
        bossExecutor != null ? bossExecutor : buildDefaultBossExecutor(),
        bossThreadCount,
        workerExecutor != null ? workerExecutor : buildDefaultWorkerExecutor(),
        workerThreadCount
    );
  }

  private ExecutorService buildDefaultBossExecutor() {
    return newCachedThreadPool(renamingDaemonThreadFactory(threadNamePattern("-boss-%s")));
  }

  private ExecutorService buildDefaultWorkerExecutor() {
    return newCachedThreadPool(renamingDaemonThreadFactory(threadNamePattern("-worker-%s")));
  }

  private String threadNamePattern(String suffix) {
    String xioName = getXioName();
    return "xio-client" + (Strings.isNullOrEmpty(xioName) ? "" : "-" + xioName) + suffix;
  }

  private ThreadFactory renamingDaemonThreadFactory(String nameFormat) {
    return new ThreadFactoryBuilder().setNameFormat(nameFormat).setDaemon(true).build();
  }
}