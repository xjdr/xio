package com.xjeffrose.xio.config;

import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.marshall.Marshallable;
import com.xjeffrose.xio.marshall.Marshaller;
import com.xjeffrose.xio.marshall.Unmarshaller;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class HostnameDeterministicRuleEngineConfig implements Marshallable {

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

  public ImmutableSet<String> getBlacklistHosts() {
    return ImmutableSet.copyOf(blacklistHosts);
  }

  public ImmutableSet<String> getWhitelistHosts() {
    return ImmutableSet.copyOf(whitelistHosts);
  }

  public String keyName() {
    return "HostnameDeterministicRuleEngineConfig";
  }

  public byte[] getBytes(Marshaller marshaller) {
    return marshaller.marshall(this);
  }

  public void putBytes(Unmarshaller unmarshaller, byte[] data) {
    unmarshaller.unmarshall(this, data);
  }

}
