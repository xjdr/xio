package com.xjeffrose.xio.config;

import java.util.HashSet;
import java.util.Set;

public class HostnameDeterministicRuleEngineConfig {

  private final Set<String> blacklistHosts = new HashSet<>();
  private final Set<String> whitelistHosts = new HashSet<>();

  public HostnameDeterministicRuleEngineConfig() {
  }

  public void blacklistHost(String host) {
    blacklistHosts.add(host);
    if (whitelistHosts.contains(host)) {
      whitelistHosts.remove(host);
    }
  }

  public void whitelistHost(String host) {
    whitelistHosts.add(host);
    if (blacklistHosts.contains(host)) {
      blacklistHosts.remove(host);
    }
  }

}
