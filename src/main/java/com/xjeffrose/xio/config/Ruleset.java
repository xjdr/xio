package com.xjeffrose.xio.config;

import com.typesafe.config.Config;
import com.xjeffrose.xio.storage.ReadProvider;
import com.xjeffrose.xio.storage.WriteProvider;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Ruleset {

  private final IpAddressDeterministicRuleEngineConfig ipRules = new IpAddressDeterministicRuleEngineConfig();
  boolean ipRulesDirty = false;

  private final String ipFilterPath;
  private final String http1FilterPath;

  public Ruleset(Config config) {
    ipFilterPath = config.getString("ipFilter.path");
    http1FilterPath = config.getString("http1Filter.path");

  }

  public long write(WriteProvider writer) {
    long recordsWritten = 0;
    if (ipRulesDirty) {
      writer.write(ipFilterPath, ipRules);
      recordsWritten += ipRules.size();
      ipRulesDirty = false;
    }

    return recordsWritten;
  }

  public void read(ReadProvider reader) {
    reader.read(ipFilterPath, ipRules);
  }

  public IpAddressDeterministicRuleEngineConfig mutateIpRules() {
    ipRulesDirty = true;
    return ipRules;
  }
}
