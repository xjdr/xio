namespace java com.xjeffrose.xio.marshall.thrift
namespace py configurator.thriftgen.HostnameRuleset

struct HostnameRuleset {
  1: required set<string> blacklistHosts
  2: required set<string> whitelistHosts
}
