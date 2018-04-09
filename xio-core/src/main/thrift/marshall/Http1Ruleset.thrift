namespace java com.xjeffrose.xio.marshall.thrift
namespace py configurator.thriftgen.Http1Ruleset

enum Http1Version {
  HTTP_1_0 = 0,
  HTTP_1_1 = 1,
}

enum Http1Method {
  CONNECT,
  DELETE,
  GET,
  HEAD,
  OPTIONS,
  PATCH,
  POST,
  PUT,
  TRACE,
}


struct Http1HeaderTuple {
  1: required string key
  2: required string value
}

struct Http1Rule {
  1: optional Http1Method method
  2: optional string uri
  3: optional Http1Version version
  4: optional list<Http1HeaderTuple> headers
}

struct Http1Ruleset {
  1: required set<Http1Rule> blacklistRules
  2: required set<Http1Rule> whitelistRules
}
