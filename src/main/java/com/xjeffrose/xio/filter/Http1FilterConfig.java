package com.xjeffrose.xio.filter;

import com.google.common.collect.ImmutableList;
import com.xjeffrose.xio.config.Http1DeterministicRuleEngineConfig;
import com.xjeffrose.xio.core.ConfigurationUpdater;
import com.xjeffrose.xio.marshall.ThriftUnmarshaller;
import io.netty.handler.codec.http.HttpRequest;
import lombok.EqualsAndHashCode;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

@EqualsAndHashCode
public class Http1FilterConfig {

  static public class Updater implements ConfigurationUpdater {
    private final String path;
    private final Consumer<Http1FilterConfig> setter;
    private final Http1DeterministicRuleEngineConfig config;
    private final ThriftUnmarshaller unmarshaller;

    public Updater(String path, Consumer<Http1FilterConfig> setter) {
      this.path = path;
      this.setter = setter;
      config = new Http1DeterministicRuleEngineConfig();
      unmarshaller = new ThriftUnmarshaller();
    }

    public String getPath() {
      return path;
    }

    public void update(byte[] data) {
      config.clear();
      unmarshaller.unmarshall(config, data);
      setter.accept(new Http1FilterConfig(config.getBlacklistRules()));
    }
  }

  private final ImmutableList<Http1DeterministicRuleEngineConfig.Rule> blacklist;

  public Http1FilterConfig() {
    blacklist = ImmutableList.of();
  }

  public Http1FilterConfig(ImmutableList<Http1DeterministicRuleEngineConfig.Rule> blacklist) {
    this.blacklist = blacklist;
  }

  public boolean denied(HttpRequest request) {
    for(Http1DeterministicRuleEngineConfig.Rule rule : blacklist) {
      if (rule.matches(request)) {
        return true;
      }
    }
    return false;
  }
}
