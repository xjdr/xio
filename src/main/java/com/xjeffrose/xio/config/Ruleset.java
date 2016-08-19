package com.xjeffrose.xio.config;

import com.xjeffrose.xio.storage.ReadProvider;
import com.xjeffrose.xio.storage.WriteProvider;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Ruleset {

  private final IpAddressDeterministicRuleEngineConfig ipRules = new IpAddressDeterministicRuleEngineConfig();
  boolean ipRulesDirty = false;

  public Ruleset() {
  }

  public long write(WriteProvider writer) {
    long recordsWritten = 0;
    if (ipRulesDirty) {
      writer.write(ipRules);
      recordsWritten += ipRules.size();
      ipRulesDirty = false;
    }

    return recordsWritten;
  }

  public void read(ReadProvider reader) {
    reader.read(ipRules);
  }

  public IpAddressDeterministicRuleEngineConfig mutateIpRules() {
    ipRulesDirty = true;
    return ipRules;
  }
}
