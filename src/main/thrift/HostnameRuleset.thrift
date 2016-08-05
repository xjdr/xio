namespace java com.xjeffrose.xio.marshall.thrift

struct HostnameRuleset {
  1: set<string> blacklistHosts
  2: set<string> whitelistHosts
}
