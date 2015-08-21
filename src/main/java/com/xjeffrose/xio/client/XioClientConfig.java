package com.xjeffrose.xio.client;

import com.google.common.net.HostAndPort;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.jboss.netty.util.Timer;

public class XioClientConfig {
  private final Map<String, Object> bootstrapOptions;
  private final HostAndPort defaultSocksProxyAddress;
  private final Timer timer;
  private final ExecutorService bossExecutor;
  private final int bossThreadCount;
  private final ExecutorService workerExecutor;
  private final int workerThreadCount;

  public XioClientConfig(Map<String, Object> bootstrapOptions,
                         HostAndPort defaultSocksProxyAddress,
                         Timer timer,
                         ExecutorService bossExecutor,
                         int bossThreadCount,
                         ExecutorService workerExecutor,
                         int workerThreadCount)

  {
    this.bootstrapOptions = bootstrapOptions;
    this.defaultSocksProxyAddress = defaultSocksProxyAddress;
    this.timer = timer;
    this.bossExecutor = bossExecutor;
    this.bossThreadCount = bossThreadCount;
    this.workerExecutor = workerExecutor;
    this.workerThreadCount = workerThreadCount;
  }

  public static XioClientConfigBuilder newBuilder() {
    return new XioClientConfigBuilder();
  }

  public Map<String, Object> getBootstrapOptions() {
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
}