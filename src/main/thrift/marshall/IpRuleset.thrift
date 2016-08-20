namespace java com.xjeffrose.xio.marshall.thrift
namespace py configurator.thriftgen.IpRuleset

struct IpRuleset {
  1: required set<binary> blacklistIps
  2: required set<binary> whitelistIps
}
