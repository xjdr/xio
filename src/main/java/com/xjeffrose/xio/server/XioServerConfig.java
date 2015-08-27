package com.xjeffrose.xio.server;


import com.google.inject.ProvidedBy;
import io.netty.channel.ChannelOption;
import io.netty.util.Timer;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@ProvidedBy(XioServerConfigProvider.class)
public class XioServerConfig {
  private final Map<ChannelOption<Object>, Object> bootstrapOptions;
  private final Timer timer;
  private final ExecutorService bossExecutor;
  private final int bossThreadCount;
  private final ExecutorService workerExecutor;
  private final int workerThreadCount;

  public XioServerConfig(Map<ChannelOption<Object>, Object> bootstrapOptions,
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

  public static XioServerConfigBuilder newBuilder() {
    return new XioServerConfigBuilder();
  }

  public Timer getTimer() {
    return timer;
  }

  public ExecutorService getBossExecutor() {
    return bossExecutor;
  }

  public Map<ChannelOption<Object>, Object> getBootstrapOptions() {
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