package com.xjeffrose.xio.config;

import com.xjeffrose.xio.config.thrift.RuleType;
import java.net.InetAddress;

public abstract class UpdateMessage {

  protected final UpdateType updateType;
  protected final Object payload;
  protected final RuleType ruleType;

  private UpdateMessage(UpdateType updateType, Object payload, RuleType ruleType) {
    this.updateType = updateType;
    this.payload = payload;
    this.ruleType = ruleType;
  }

  public abstract void process(UpdateHandler handler);

  public static class IpRuleUpdate extends UpdateMessage {
    IpRuleUpdate(UpdateType updateType, InetAddress address, RuleType ruleType) {
      super(updateType, address, ruleType);
    }

    @Override
    public void process(UpdateHandler handler) {
      handler.process(updateType, (InetAddress) payload, ruleType);
    }
  }

  public static class Http1RuleUpdate extends UpdateMessage {
    Http1RuleUpdate(
        UpdateType updateType,
        Http1DeterministicRuleEngineConfig.Rule http1Rule,
        RuleType ruleType) {
      super(updateType, http1Rule, ruleType);
    }

    @Override
    public void process(UpdateHandler handler) {
      handler.process(updateType, (Http1DeterministicRuleEngineConfig.Rule) payload, ruleType);
    }
  }

  public static UpdateMessage addIpRule(InetAddress address, RuleType ruleType) {
    return new IpRuleUpdate(UpdateType.Add, address, ruleType);
  }

  public static UpdateMessage removeIpRule(InetAddress address) {
    return new IpRuleUpdate(UpdateType.Remove, address, RuleType.blacklist);
  }

  public static UpdateMessage addHttp1Rule(
      Http1DeterministicRuleEngineConfig.Rule http1Rule, RuleType ruleType) {
    return new Http1RuleUpdate(UpdateType.Add, http1Rule, ruleType);
  }

  public static UpdateMessage removeHttp1Rule(Http1DeterministicRuleEngineConfig.Rule http1Rule) {
    return new Http1RuleUpdate(UpdateType.Remove, http1Rule, RuleType.blacklist);
  }
}
