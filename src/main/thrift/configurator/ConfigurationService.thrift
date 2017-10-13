namespace java com.xjeffrose.xio.config.thrift
namespace py configurator.thriftgen.ConfigurationService

include "../marshall/Http1Ruleset.thrift"

enum RuleType {
  blacklist,
  whitelist
}

struct IpRule {
  1: binary ipAddress
}

struct Result {
  1: bool success
  2: string errorReason
}

service ConfigurationService {
  Result addIpRule(1:IpRule ipRule, 2:RuleType ruleType),
  Result removeIpRule(1:IpRule ipRule),
  Result addHttp1Rule(1:Http1Ruleset.Http1Rule http1Rule, 2:RuleType ruleType),
  Result removeHttp1Rule(1:Http1Ruleset.Http1Rule http1Rule),
}
