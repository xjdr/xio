package com.xjeffrose.xio.client;

import com.google.common.net.HostAndPort;
import com.xjeffrose.xio.core.XioSecurityFactory;
import io.netty.channel.ChannelOption;
import io.netty.util.Timer;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class XioClientConfig {
  private final Map<ChannelOption<Object>, Object> bootstrapOptions;
  private final HostAndPort defaultSocksProxyAddress;
  private final Timer timer;
  private final ExecutorService bossExecutor;
  private final int bossThreadCount;
  private final ExecutorService workerExecutor;
  private final int workerThreadCount;
  private final XioSecurityFactory securityFactory;

  public XioClientConfig(Map<ChannelOption<Object>, Object> bootstrapOptions,
                         HostAndPort defaultSocksProxyAddress,
                         Timer timer,
                         ExecutorService bossExecutor,
                         int bossThreadCount,
                         ExecutorService workerExecutor,
                         int workerThreadCount,
                         XioSecurityFactory securityFactory) {

    this.bootstrapOptions = bootstrapOptions;
    this.defaultSocksProxyAddress = defaultSocksProxyAddress;
    this.timer = timer;
    this.bossExecutor = bossExecutor;
    this.bossThreadCount = bossThreadCount;
    this.workerExecutor = workerExecutor;
    this.workerThreadCount = workerThreadCount;
    this.securityFactory = securityFactory;
  }

  public static XioClientConfigBuilder newBuilder() {
    return new XioClientConfigBuilder();
  }

  public Map<ChannelOption<Object>, Object> getBootstrapOptions() {
    return bootstrapOptions;
  }

  public ExecutorService getBossExecutor() {
    return bossExecutor;
  }

  public int getBossThreadCount() {
    return bossThreadCount;
  }

  public HostAndPort getDefaultSocksProxyAddress() {
    return defaultSocksProxyAddress;
  }

  public Timer getTimer() {
    return timer;
  }

  public ExecutorService getWorkerExecutor() {
    return workerExecutor;
  }

  public int getWorkerThreadCount() {
    return workerThreadCount;
  }

  public XioSecurityFactory getSecurityFactory() {
    return securityFactory;
  }
}