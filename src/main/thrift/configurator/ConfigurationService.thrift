namespace java com.xjeffrose.xio.config.thrift
namespace py configurator.thriftgen.ConfigurationService

enum RuleType {
  blacklist,
  whitelist
}

struct IpRule {
  1: binary ipAddress
  2: RuleType ruleType
}

struct Result {
  1: bool success
  2: string errorReason
}

service ConfigurationService {
  Result addIpRule(1:IpRule ipRule),
  Result removeIpRule(1:IpRule ipRule);
}
