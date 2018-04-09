package com.xjeffrose.xio.marshall;

import com.google.common.collect.HashMultimap;
import com.xjeffrose.xio.config.HostnameDeterministicRuleEngineConfig;
import com.xjeffrose.xio.config.Http1DeterministicRuleEngineConfig;
import com.xjeffrose.xio.config.IpAddressDeterministicRuleEngineConfig;
import com.xjeffrose.xio.marshall.thrift.HostnameRuleset;
import com.xjeffrose.xio.marshall.thrift.Http1HeaderTuple;
import com.xjeffrose.xio.marshall.thrift.Http1Method;
import com.xjeffrose.xio.marshall.thrift.Http1Rule;
import com.xjeffrose.xio.marshall.thrift.Http1Ruleset;
import com.xjeffrose.xio.marshall.thrift.Http1Version;
import com.xjeffrose.xio.marshall.thrift.IpRuleset;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TCompactProtocol;

public class ThriftMarshaller implements Marshaller {

  TSerializer serializer;

  private byte[] serialize(TBase<?, ?> message) {
    try {
      return serializer.serialize(message);
    } catch (TException e) {
      throw new RuntimeException(e);
    }
  }

  public ThriftMarshaller() {
    serializer = new TSerializer(new TCompactProtocol.Factory());
  }

  public byte[] marshall(HostnameDeterministicRuleEngineConfig config) {
    HostnameRuleset message = new HostnameRuleset();

    message.setBlacklistHosts(new HashSet<String>());
    for (String address : config.getBlacklistHosts()) {
      message.addToBlacklistHosts(address);
    }

    message.setWhitelistHosts(new HashSet<String>());
    for (String address : config.getWhitelistHosts()) {
      message.addToWhitelistHosts(address);
    }

    return serialize(message);
  }

  private Http1Method build(HttpMethod method) {
    if (method != null) {
      if (method.equals(HttpMethod.CONNECT)) {
        return Http1Method.CONNECT;
      } else if (method.equals(HttpMethod.DELETE)) {
        return Http1Method.DELETE;
      } else if (method.equals(HttpMethod.GET)) {
        return Http1Method.GET;
      } else if (method.equals(HttpMethod.HEAD)) {
        return Http1Method.HEAD;
      } else if (method.equals(HttpMethod.OPTIONS)) {
        return Http1Method.OPTIONS;
      } else if (method.equals(HttpMethod.PATCH)) {
        return Http1Method.PATCH;
      } else if (method.equals(HttpMethod.POST)) {
        return Http1Method.POST;
      } else if (method.equals(HttpMethod.PUT)) {
        return Http1Method.PUT;
      } else if (method.equals(HttpMethod.TRACE)) {
        return Http1Method.TRACE;
      }
    }
    return null;
  }

  private Http1Version build(HttpVersion version) {
    if (version != null) {
      if (version.equals(HttpVersion.HTTP_1_0)) {
        return Http1Version.HTTP_1_0;
      } else if (version.equals(HttpVersion.HTTP_1_1)) {
        return Http1Version.HTTP_1_1;
      }
    }
    return null;
  }

  private List<Http1HeaderTuple> build(HashMultimap<String, String> headers) {
    if (headers != null && headers.size() > 0) {
      ArrayList<Http1HeaderTuple> result = new ArrayList<Http1HeaderTuple>();

      headers
          .entries()
          .stream()
          .forEach(e -> result.add(new Http1HeaderTuple(e.getKey(), e.getValue())));

      return result;
    }
    return null;
  }

  private Http1Rule build(Http1DeterministicRuleEngineConfig.Rule rule) {
    Http1Rule message = new Http1Rule();
    message.setMethod(build(rule.getMethod()));
    message.setUri(rule.getUri());
    message.setVersion(build(rule.getVersion()));
    message.setHeaders(build(rule.getHeaders()));
    return message;
  }

  public byte[] marshall(Http1DeterministicRuleEngineConfig config) {
    Http1Ruleset message = new Http1Ruleset();

    message.setBlacklistRules(new HashSet<Http1Rule>());
    for (Http1DeterministicRuleEngineConfig.Rule rule : config.getBlacklistRules()) {
      message.addToBlacklistRules(build(rule));
    }

    message.setWhitelistRules(new HashSet<Http1Rule>());
    for (Http1DeterministicRuleEngineConfig.Rule rule : config.getWhitelistRules()) {
      message.addToWhitelistRules(build(rule));
    }

    return serialize(message);
  }

  public byte[] marshall(IpAddressDeterministicRuleEngineConfig config) {
    IpRuleset message = new IpRuleset();

    message.setBlacklistIps(new HashSet<ByteBuffer>());
    for (InetAddress address : config.getBlacklistIps()) {
      message.addToBlacklistIps(ByteBuffer.wrap(address.getAddress()));
    }

    message.setWhitelistIps(new HashSet<ByteBuffer>());
    for (InetAddress address : config.getWhitelistIps()) {
      message.addToWhitelistIps(ByteBuffer.wrap(address.getAddress()));
    }

    return serialize(message);
  }
}
