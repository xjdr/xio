package com.xjeffrose.xio.config;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class IpAddressDeterministicRuleEngineConfig {

  private final Set<InetAddress> blacklistIps = new HashSet<>();
  private final Set<InetAddress> whitelistIps = new HashSet<>();

  public IpAddressDeterministicRuleEngineConfig() {
  }

  public void blacklistIp(InetAddress address) {
    blacklistIps.add(address);
    if (whitelistIps.contains(address)) {
      whitelistIps.remove(address);
    }
  }

  public void whitelistIp(InetAddress address) {
    whitelistIps.add(address);
    if (blacklistIps.contains(address)) {
      blacklistIps.remove(address);
    }
  }

}
