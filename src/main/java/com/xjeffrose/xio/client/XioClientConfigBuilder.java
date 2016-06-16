package com.xjeffrose.xio.client;


import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xjeffrose.xio.server.XioConfigBuilderBase;
import com.xjeffrose.xio.server.XioNoOpSecurityFactory;
import com.xjeffrose.xio.server.XioSecurityFactory;
import com.xjeffrose.xio.core.XioTimer;
import io.netty.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;


import static java.util.concurrent.Executors.newCachedThreadPool;

/*
 * Hooks for configuring various parts of Netty.
 */
public class XioClientConfigBuilder extends XioConfigBuilderBase<XioClientConfigBuilder> {
//  private final ChannelConfig socketChannelConfig = (ChannelConfig) Proxy.newProxyInstance(
//      getClass().getClassLoader(),
//      new Class<?>[]{ChannelConfig.class},
//      new XioConfigBuilderBase.Magic("")
//  );

  private XioSecurityFactory securityFactory = null;
  private HostAndPort defaultSocksProxyAddress = null;


  public XioClientConfigBuilder() {
//    getSocketChannelConfig().setOption(ChannelOption.TCP_NODELAY, true);
  }

//  public ChannelConfig getSocketChannelConfig() {
//    return socketChannelConfig;
//  }

  public XioClientConfigBuilder setDefaultSocksProxyAddress(HostAndPort defaultSocksProxyAddress) {
    this.defaultSocksProxyAddress = defaultSocksProxyAddress;
    return this;
  }

  public XioClientConfigBuilder setSecurityFactory(XioSecurityFactory xioSecurityFactory) {
    this.securityFactory = xioSecurityFactory;
    return this;
  }

  public XioSecurityFactory getSecurityFactory() {
    return securityFactory;
  }

  public XioClientConfig build() {
    Timer timer = getTimer();
    ExecutorService bossExecutor = getBossExecutor();
    int bossThreadCount = getBossThreadCount();
    ExecutorService workerExecutor = getWorkerExecutor();
    int workerThreadCount = getWorkerThreadCount();
    XioSecurityFactory securityFactory = getSecurityFactory();

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
