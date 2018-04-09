package com.xjeffrose.xio.config;

import com.xjeffrose.xio.config.thrift.RuleType;
import java.net.InetAddress;

interface UpdateHandler {

  long commit();

  void process(UpdateType updateType, InetAddress address, RuleType ruleType);

  void process(
      UpdateType updateType, Http1DeterministicRuleEngineConfig.Rule http1Rule, RuleType ruleType);
}
