package com.xjeffrose.xio.core;


import com.google.inject.ProvidedBy;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.jboss.netty.util.Timer;

@ProvidedBy(DefaultNettyServerConfigProvider.class)
public class NettyServerConfig {
  private final Map<String, Object> bootstrapOptions;
  private final Timer timer;
  private final ExecutorService bossExecutor;
  private final int bossThreadCount;
  private final ExecutorService workerExecutor;
  private final int workerThreadCount;

  public NettyServerConfig(Map<String, Object> bootstrapOptions,
                           Timer timer,
                           ExecutorService bossExecutor,
                           int bossThreadCount,
                           ExecutorService workerExecutor,
                           int workerThreadCount) {
    this.bootstrapOptions = bootstrapOptions;
    this.timer = timer;
    this.bossExecutor = bossExecutor;
    this.bossThreadCount = bossThreadCount;
    this.workerExecutor = workerExecutor;
    this.workerThreadCount = workerThreadCount;
  }

  public static NettyServerConfigBuilder newBuilder() {
    return new NettyServerConfigBuilder();
  }

  public Timer getTimer() {
    return timer;
  }

  public ExecutorService getBossExecutor() {
    return bossExecutor;
  }

  public Map<String, Object> getBootstrapOptions() {
    return bootstrapOptions;
  }

  public int getBossThreadCount() {
    return bossThreadCount;
  }

  public ExecutorService getWorkerExecutor() {
    return workerExecutor;
  }

  public int getWorkerThreadCount() {
    return workerThreadCount;
  }
}