package com.xjeffrose.xio.config;

import com.google.common.collect.HashMultimap;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode
public class Http1DeterministicRuleEngineConfig {

  @EqualsAndHashCode
  public class Rule {
    // request line
    private final HttpMethod method;
    private final String uri;
    private final HttpVersion version;
    // headers
    private HashMultimap<String, String> headers;

    Rule(HttpMethod method, String uri, HttpVersion version, HashMultimap<String, String> headers) {
      this.method = method;
      this.uri = uri;
      this.version = version;
      this.headers = headers;
    }

    public boolean matches(HttpRequest request) {
      if (method != null && !method.equals(request.method())) {
        return false;
      }
      if (uri != null && !uri.equals(request.uri())) {
        return false;
      }
      if (version != null && !version.equals(request.protocolVersion())) {
        return false;
      }
      // TODO(CK): ven diagram
      if (headers != null && headers.size() > 0) {
        for (String key : headers.keySet()) {
          if (request.headers().contains(key)) {
            boolean found = false;
            for (String value : request.headers().getAll(key)) {
              if (headers.get(key).contains(value)) {
                found = true;
                break;
              }
            }
            if (!found) {
              return false;
            }
          } else {
            return false;
          }
        }
      }
      return true;
    }
  }

  private final Set<Rule> blacklistRules = new HashSet<>();
  private final Set<Rule> whitelistRules = new HashSet<>();

  public void blacklistRule(Rule rule) {
    blacklistRules.add(rule);
    if (whitelistRules.contains(rule)) {
      whitelistRules.remove(rule);
    }
  }

  public void whitelistRule(Rule rule) {
    whitelistRules.add(rule);
    if (blacklistRules.contains(rule)) {
      blacklistRules.remove(rule);
    }
  }

}
