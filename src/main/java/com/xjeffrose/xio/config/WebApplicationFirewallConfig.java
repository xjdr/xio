package com.xjeffrose.xio.config;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class WebApplicationFirewallConfig {

  private final Map<String, Http1DeterministicRuleEngineConfig> blacklistConfigs = new HashMap<>();
  private final Map<String, Http1DeterministicRuleEngineConfig> whitelistConfigs = new HashMap<>();

  public WebApplicationFirewallConfig() {
  }

  public void blacklistIp(String application, Http1DeterministicRuleEngineConfig config) {
    blacklistConfigs.put(application, config);
    if (whitelistConfigs.containsKey(application) && whitelistConfigs.get(application).equals(config)) {
      whitelistConfigs.remove(application);
    }
  }

  public void whitelistIp(String application, Http1DeterministicRuleEngineConfig config) {
    whitelistConfigs.put(application, config);
    if (blacklistConfigs.containsKey(application) && blacklistConfigs.get(application).equals(config)) {
      blacklistConfigs.remove(application);
    }
  }

}
