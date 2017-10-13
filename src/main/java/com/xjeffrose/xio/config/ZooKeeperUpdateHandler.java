package com.xjeffrose.xio.config;

import com.xjeffrose.xio.config.thrift.RuleType;
import com.xjeffrose.xio.storage.ZooKeeperWriteProvider;

import java.net.InetAddress;

public class ZooKeeperUpdateHandler implements UpdateHandler {

  private final ZooKeeperWriteProvider writer;
  private final Ruleset rules;

  public ZooKeeperUpdateHandler(ZooKeeperWriteProvider writer, Ruleset rules) {
    this.writer = writer;
    this.rules = rules;
  }

  @Override
  public long commit() {
    return rules.write(writer);
  }

  @Override
  public void process(UpdateType updateType, InetAddress address, RuleType ruleType) {
    if (updateType == UpdateType.Add) {
      if (ruleType == RuleType.blacklist) {
        rules.mutateIpRules().blacklistIp(address);
      } else if (ruleType == RuleType.whitelist) {
        rules.mutateIpRules().whitelistIp(address);
      }
    } else if (updateType == UpdateType.Remove) {
      rules.mutateIpRules().remove(address);
    }
  }

  @Override
  public void process(UpdateType updateType, Http1DeterministicRuleEngineConfig.Rule http1Rule, RuleType ruleType) {
    if (updateType == UpdateType.Add) {
      if (ruleType == RuleType.blacklist) {
        rules.mutateHttp1Rules().blacklistRule(http1Rule);
      } else if (ruleType == RuleType.whitelist) {
        rules.mutateHttp1Rules().whitelistRule(http1Rule);
      }
    } else if (updateType == UpdateType.Remove) {
      rules.mutateHttp1Rules().remove(http1Rule);
    }
  }
  
}
