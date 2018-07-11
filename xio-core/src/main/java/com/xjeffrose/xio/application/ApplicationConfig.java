package com.xjeffrose.xio.application;

import com.google.common.annotations.VisibleForTesting;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ServerChannelConfiguration;
import com.xjeffrose.xio.core.NullZkClient;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.util.internal.PlatformDependent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationConfig {

  private final Config config;
  @Getter private final String name;
  @Getter private final int bossThreads;
  @Getter private final String bossNameFormat;
  @Getter private final int workerThreads;
  @Getter private final String workerNameFormat;
  @Getter private final String zookeeperCluster;
  @Getter private final String ipFilterPath;
  @Getter private final String http1FilterPath;
  @Getter private final double globalSoftReqPerSec;
  @Getter private final double globalHardReqPerSec;
  @Getter private final double softReqPerSec;
  @Getter private final double hardReqPerSec;
  @Getter private final int rateLimiterPoolSize;
  @Getter private final int clientPoolSize;
  @Getter private final XioTracing tracing;

  @Getter
  private final Map<String, List<Double>> clientRateLimitOverride =
      PlatformDependent.newConcurrentHashMap();

  private ApplicationConfig(Config config, XioTracing tracing) {
    this.config = config;
    name = config.getString("name");
    bossThreads = config.getInt("settings.bossThreads");
    bossNameFormat = config.getString("settings.bossNameFormat");
    workerThreads = config.getInt("settings.workerThreads");
    workerNameFormat = config.getString("settings.workerNameFormat");
    zookeeperCluster = config.getString("settings.zookeeper.cluster");
    ipFilterPath = config.getString("settings.configurationManager.ipFilter.path");
    http1FilterPath = config.getString("settings.configurationManager.http1Filter.path");
    globalSoftReqPerSec = config.getDouble("settings.global_soft_req_per_sec");
    globalHardReqPerSec = config.getDouble("settings.global_hard_req_per_sec");
    softReqPerSec = config.getDouble("settings.soft_req_per_sec");
    hardReqPerSec = config.getDouble("settings.hard_req_per_sec");
    rateLimiterPoolSize = config.getInt("settings.rate_limiter_pool_size");
    clientPoolSize = config.getInt("clientLimits.clientPoolSize");
    this.tracing = tracing;
  }

  @VisibleForTesting
  public ApplicationConfig(Config config, Function<Config, XioTracing> tracingSupplier) {
    this(config, tracingSupplier.apply(config));
  }

  public ApplicationConfig(Config config) {
    this(config, new XioTracing(config));
  }

  public static ApplicationConfig fromConfig(String key, Config config) {
    return new ApplicationConfig(config.getConfig(key));
  }

  public static ApplicationConfig fromConfig(String key) {
    return fromConfig(key, ConfigFactory.load());
  }

  // TODO(CK): parse servers at construction time
  public Config getServer(String server) {
    try {
      return config.getConfig("servers").getConfig(server);
    } catch (ConfigException e) {
      String servers =
          config
              .getConfig("servers")
              .root()
              .entrySet()
              .stream()
              .map(i -> i.getKey())
              .collect(java.util.stream.Collectors.joining(", "));
      log.error("Invalid server '{}', available servers: {}", server, servers);
      throw e;
    }
  }

  // TODO(CK): parse settings at construction time
  public Config settings() {
    return config.getConfig("settings");
  }

  public ServerChannelConfiguration serverChannelConfig() {
    return ChannelConfiguration.serverConfig(
        bossThreads, bossNameFormat, workerThreads, workerNameFormat);
  }

  // TODO(CK): refactor this into a ZooKeeperConfig that the ZkClient constructor will accept
  public ZkClient zookeeperClient() {
    if (zookeeperCluster.isEmpty()) {
      return new NullZkClient();
    } else {
      if (zookeeperCluster.startsWith("exhibitor:")) {
        String[] values = zookeeperCluster.replace("exhibitor:", "").split(":");
        int restPort = Integer.parseInt(values[0]);
        Collection<String> serverSet = Arrays.asList(values[1].split(","));
        return ZkClient.fromExhibitor(serverSet, restPort);
      } else {
        return new ZkClient(zookeeperCluster);
      }
    }
  }
}
