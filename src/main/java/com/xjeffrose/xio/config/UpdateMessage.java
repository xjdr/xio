package com.xjeffrose.xio.config;

import com.xjeffrose.xio.config.thrift.RuleType;
import java.net.InetAddress;

abstract public class UpdateMessage {

  protected final UpdateType updateType;
  protected final Object payload;
  protected final RuleType ruleType;

  private UpdateMessage(UpdateType updateType, Object payload, RuleType ruleType) {
    this.updateType = updateType;
    this.payload = payload;
    this.ruleType = ruleType;
  }

  abstract public void process(UpdateHandler handler);

  static public class IpRuleUpdate extends UpdateMessage {
    IpRuleUpdate(UpdateType updateType, InetAddress address, RuleType ruleType) {
      super(updateType, address, ruleType);
    }

    @Override
    public void process(UpdateHandler handler) {
      handler.process(updateType, (InetAddress)payload, ruleType);
    }
  }

  static public class Http1RuleUpdate extends UpdateMessage {
    Http1RuleUpdate(UpdateType updateType, Http1DeterministicRuleEngineConfig.Rule http1Rule, RuleType ruleType) {
      super(updateType, http1Rule, ruleType);
    }

    @Override
    public void process(UpdateHandler handler) {
      handler.process(updateType, (Http1DeterministicRuleEngineConfig.Rule)payload, ruleType);
    }
  }

  static public UpdateMessage addIpRule(InetAddress address, RuleType ruleType) {
    return new IpRuleUpdate(UpdateType.Add, address, ruleType);
  }

  static public UpdateMessage removeIpRule(InetAddress address) {
    return new IpRuleUpdate(UpdateType.Remove, address, RuleType.blacklist);
  }

  static public UpdateMessage addHttp1Rule(Http1DeterministicRuleEngineConfig.Rule http1Rule, RuleType ruleType) {
    return new Http1RuleUpdate(UpdateType.Add, http1Rule, ruleType);
  }

  static public UpdateMessage removeHttp1Rule(Http1DeterministicRuleEngineConfig.Rule http1Rule) {
    return new Http1RuleUpdate(UpdateType.Remove, http1Rule, RuleType.blacklist);
  }
}
