package com.xjeffrose.xio.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import io.netty.channel.ChannelOption;
import io.netty.util.Timer;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.net.InetSocketAddress;

public class XioServerConfig {
  // old
  private final Map<ChannelOption<Object>, Object> bootstrapOptions;
  private final Timer timer;
  private final ExecutorService bossExecutor;
  private final int bossThreadCount;
  private final ExecutorService workerExecutor;
  private final int workerThreadCount;
  // new
  private String name;
  private InetSocketAddress bindAddress;
  private XioServerLimits limits;
  private TlsConfig tls;

  public XioServerConfig(Config config) {
    bootstrapOptions = null;
    timer = null;
    bossExecutor = null;
    bossThreadCount = config.getInt("settings.bossThreads");
    workerExecutor = null;
    workerThreadCount = config.getInt("settings.workerThreads");
    name = config.getString("name");
    bindAddress = new InetSocketAddress(config.getString("settings.bindHost"), config.getInt("settings.bindPort"));
    limits = new XioServerLimits(config.getConfig("limits"));
    tls = new TlsConfig(config.getConfig("settings.tls"));
  }

  static public XioServerConfig fromConfig(String key, Config config) {
    return new XioServerConfig(config.getConfig(key));
  }
  static public XioServerConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

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

  public InetSocketAddress getBindAddress() {
    return bindAddress;
  }

  public String getName() {
    return name;
  }

  public XioServerLimits limits() {
    return limits;
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

  public String getCert() {
    return tls.getCert();
  }

  public String getKey() {
    return tls.getKey();
  }

}
