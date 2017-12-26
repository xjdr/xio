package com.xjeffrose.xio.config;

import com.xjeffrose.xio.config.thrift.IpRule;
import com.xjeffrose.xio.config.thrift.Result;
import com.xjeffrose.xio.config.thrift.RuleType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IpRules {

  private final Map<InetAddress, RuleType> rules = new HashMap<>();

  public IpRules(Ruleset existing) {
    existing.populateIpRules(rules);
  }

  public Result add(IpRule ipRule, RuleType ruleType, BlockingQueue<UpdateMessage> workLoad) {
    try {
      InetAddress address = InetAddress.getByAddress(ipRule.getIpAddress());
      log.debug("address {}", address.getHostAddress());
      RuleType existingRuleType = rules.get(address);
      if (existingRuleType != null && existingRuleType.equals(ruleType)) {
        return new Result(
            false, "address " + address.getHostAddress() + " already on " + existingRuleType);
      } else {
        workLoad.put(UpdateMessage.addIpRule(address, ruleType));
        rules.put(address, ruleType);
        log.debug("rules {}", rules);
      }
    } catch (UnknownHostException | InterruptedException e) {
      log.error("addIpRule couldn't add {}", ipRule, e);
      return new Result(false, e.getMessage());
    }

    return new Result(true, "");
  }

  public Result remove(IpRule ipRule, BlockingQueue<UpdateMessage> workLoad) {
    try {
      InetAddress address = InetAddress.getByAddress(ipRule.getIpAddress());
      log.debug("address {}", address.getHostAddress());
      if (!rules.containsKey(address)) {
        return new Result(false, "nothing to remove for address " + address.getHostAddress());
      } else {
        workLoad.put(UpdateMessage.removeIpRule(address));
        rules.remove(address);
      }
    } catch (UnknownHostException | InterruptedException e) {
      log.error("addIpRule couldn't add {}", ipRule, e);
      return new Result(false, e.getMessage());
    }
    return new Result(true, "");
  }
}
