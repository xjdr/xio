namespace java com.xjeffrose.xio.marshall.thrift

struct IpRuleset {
  1: set<binary> blacklistIps
  2: set<binary> whitelistIps
}
