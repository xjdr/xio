package com.xjeffrose.xio.filter;

import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.config.IpAddressDeterministicRuleEngineConfig;
import com.xjeffrose.xio.core.ConfigurationUpdater;
import com.xjeffrose.xio.marshall.ThriftUnmarshaller;
import java.net.InetAddress;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class IpFilterConfig {

  public static class Updater implements ConfigurationUpdater {
    private final String path;
    private final Consumer<IpFilterConfig> setter;
    private final IpAddressDeterministicRuleEngineConfig config;
    private final ThriftUnmarshaller unmarshaller;

    public Updater(String path, Consumer<IpFilterConfig> setter) {
      this.path = path;
      this.setter = setter;
      config = new IpAddressDeterministicRuleEngineConfig();
      unmarshaller = new ThriftUnmarshaller();
    }

    public String getPath() {
      return path;
    }

    public void update(byte[] data) {
      config.clear();
      unmarshaller.unmarshall(config, data);
      setter.accept(new IpFilterConfig(config.getBlacklistIps(), config.getWhitelistIps()));
    }
  }

  @Getter private final ImmutableSet<InetAddress> blacklist;
  @Getter private final ImmutableSet<InetAddress> whitelist;

  public IpFilterConfig() {
    blacklist = ImmutableSet.of();
    whitelist = ImmutableSet.of();
  }

  public IpFilterConfig(ImmutableSet<InetAddress> blacklist, ImmutableSet<InetAddress> whitelist) {
    this.blacklist = blacklist;
    this.whitelist = whitelist;
  }
}
