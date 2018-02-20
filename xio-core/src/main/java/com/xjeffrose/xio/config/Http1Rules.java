package com.xjeffrose.xio.config;

import com.xjeffrose.xio.config.thrift.Result;
import com.xjeffrose.xio.config.thrift.RuleType;
import com.xjeffrose.xio.marshall.ThriftUnmarshaller;
import com.xjeffrose.xio.marshall.thrift.Http1Rule;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Http1Rules {

  private final Map<Http1DeterministicRuleEngineConfig.Rule, RuleType> rules = new HashMap<>();

  public Http1Rules(Ruleset existing) {
    existing.populateHttp1Rules(rules);
  }

  public Result add(Http1Rule http1Rule, RuleType ruleType, BlockingQueue<UpdateMessage> workLoad) {
    try {
      Http1DeterministicRuleEngineConfig.Rule newRule = ThriftUnmarshaller.build(http1Rule);
      log.debug("rule {}", newRule);
      RuleType existingRuleType = rules.get(newRule);
      if (existingRuleType != null && existingRuleType.equals(ruleType)) {
        return new Result(false, "rule " + newRule + " already on " + existingRuleType);
      } else {
        workLoad.put(UpdateMessage.addHttp1Rule(newRule, ruleType));
        rules.put(newRule, ruleType);
        log.debug("rules {}", rules);
      }
    } catch (InterruptedException e) {
      log.error("addIpRule couldn't add {}", http1Rule, e);
      return new Result(false, e.getMessage());
    }

    return new Result(true, "");
  }

  public Result remove(Http1Rule http1Rule, BlockingQueue<UpdateMessage> workLoad) {
    try {
      Http1DeterministicRuleEngineConfig.Rule newRule = ThriftUnmarshaller.build(http1Rule);
      log.debug("rule {}", newRule);
      if (!rules.containsKey(newRule)) {
        return new Result(false, "nothing to remove for rule " + newRule);
      } else {
        workLoad.put(UpdateMessage.removeHttp1Rule(newRule));
        rules.remove(newRule);
      }
    } catch (InterruptedException e) {
      log.error("addIpRule couldn't add {}", http1Rule, e);
      return new Result(false, e.getMessage());
    }
    return new Result(true, "");
  }
}
