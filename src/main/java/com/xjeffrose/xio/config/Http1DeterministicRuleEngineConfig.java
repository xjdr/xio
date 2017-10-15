package com.xjeffrose.xio.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.xjeffrose.xio.marshall.Marshallable;
import com.xjeffrose.xio.marshall.Marshaller;
import com.xjeffrose.xio.marshall.Unmarshaller;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class Http1DeterministicRuleEngineConfig implements Marshallable {

  @EqualsAndHashCode
  static public class Rule {
    // request line
    @Getter
    private final HttpMethod method;
    @Getter
    private final String uri;
    @Getter
    private final HttpVersion version;
    // headers
    @Getter
    private HashMultimap<String, String> headers;

    public Rule(HttpMethod method, String uri, HttpVersion version, HashMultimap<String, String> headers) {
      this.method = method;
      this.uri = uri;
      this.version = version;
      this.headers = headers;
    }

    /**
     * For every header key defined in the headers multimap do the following:
     *  * return false if the request doesn't have a header for that key
     *  * iterate all of the request header values for that key
     *  * if none of the request header values match any of the multimap values, return false
     *  * otherwise return true
     */
    private boolean matchHeaders(HttpRequest request) {
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

      return matchHeaders(request);
    }

    @Override
    public String toString() {
      String headers = "Any";
      if (this.headers != null) {
        headers = this.headers.toString();
      }

      String method = "Any";
      if (this.method != null) {
        method = this.method.toString();
      }

      String version = "Any";
      if (this.version != null) {
        version = this.version.toString();
      }

      String uri = "Any";
      if (this.uri != null) {
        uri = this.uri;
      }

      return "headers: [" + headers + "] method: [" + method + "] version: [" + version + "] uri: [" + uri + "]";
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

  public ImmutableList<Rule> getBlacklistRules() {
    return ImmutableList.copyOf(blacklistRules);
  }

  public ImmutableList<Rule> getWhitelistRules() {
    return ImmutableList.copyOf(whitelistRules);
  }

  public String keyName() {
    return "Http1DeterministicRuleEngineConfig";
  }

  public byte[] getBytes(Marshaller marshaller) {
    return marshaller.marshall(this);
  }

  public void putBytes(Unmarshaller unmarshaller, byte[] data) {
    unmarshaller.unmarshall(this, data);
  }

  public void remove(Rule rule) {
    if (blacklistRules.contains(rule)) {
      blacklistRules.remove(rule);
    }
    if (whitelistRules.contains(rule)) {
      whitelistRules.remove(rule);
    }
  }

  public void clear() {
    blacklistRules.clear();
    whitelistRules.clear();
  }

  public long size() {
    return blacklistRules.size() + whitelistRules.size();
  }
}
