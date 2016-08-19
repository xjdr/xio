namespace java com.xjeffrose.xio.marshall.thrift

struct HostnameRuleset {
  1: required set<string> blacklistHosts
  2: required set<string> whitelistHosts
}
