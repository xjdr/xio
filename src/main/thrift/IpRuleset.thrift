namespace java com.xjeffrose.xio.marshall.thrift

struct IpRuleset {
  1: required set<binary> blacklistIps
  2: required set<binary> whitelistIps
}
