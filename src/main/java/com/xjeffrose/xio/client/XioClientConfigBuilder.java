package com.xjeffrose.xio.client;


import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.xjeffrose.xio.core.XioConfigBuilderBase;
import com.xjeffrose.xio.core.XioNoOpSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityFactory;
import com.xjeffrose.xio.core.XioTimer;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannelConfig;
import org.jboss.netty.util.Timer;

import static java.util.concurrent.Executors.newCachedThreadPool;

/*
 * Hooks for configuring various parts of Netty.
 */
public class XioClientConfigBuilder extends XioConfigBuilderBase<XioClientConfigBuilder> {
  private XioSecurityFactory securityFactory;
  private final NioSocketChannelConfig socketChannelConfig = (NioSocketChannelConfig) Proxy.newProxyInstance(
      getClass().getClassLoader(),
      new Class<?>[]{NioSocketChannelConfig.class},
      new XioConfigBuilderBase.Magic("")
  );
  private HostAndPort defaultSocksProxyAddress = null;

  @Inject
  public XioClientConfigBuilder() {
    // Thrift turns TCP_NODELAY by default, and turning it off can have latency implications
    // so let's turn it on by default as well. It can still be switched off by explicitly
    // calling setTcpNodelay(false) after construction.
    getSocketChannelConfig().setTcpNoDelay(true);
  }

  /**
   * Returns an implementation of {@link NioSocketChannelConfig} which will be applied to all {@link
   * org.jboss.netty.channel.socket.nio.NioSocketChannel} instances created for client connections.
   *
   * @return A mutable {@link NioSocketChannelConfig}
   */
  public NioSocketChannelConfig getSocketChannelConfig() {
    return socketChannelConfig;
  }

  /**
   * A default SOCKS proxy address for client connections. Defaults to {@code null} if not
   * supplied.
   *
   * @param defaultSocksProxyAddress The address of the SOCKS proxy server
   * @return This builder
   */
  public XioClientConfigBuilder setDefaultSocksProxyAddress(HostAndPort defaultSocksProxyAddress) {
    this.defaultSocksProxyAddress = defaultSocksProxyAddress;
    return this;
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
        workerThreadCount,
        securityFactory != null ? securityFactory : new XioNoOpSecurityFactory()
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