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
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;

public class ThriftUnmarshaller implements Unmarshaller {

  TDeserializer deserializer;

  private void deserialize(TBase<?, ?> message, byte[] data) {
    try {
      deserializer.deserialize(message, data);
    } catch (TException e) {
      throw new RuntimeException(e);
    }
  }

  public ThriftUnmarshaller() {
    deserializer = new TDeserializer(new TCompactProtocol.Factory());
  }

  public byte[] getBytes(ByteBuffer buffer) {
    int size = buffer.remaining();

    byte[] result = new byte[size];

    buffer.get(result);
    return result;
  }

  public void unmarshall(HostnameDeterministicRuleEngineConfig config, byte[] data) {
    HostnameRuleset message = new HostnameRuleset();

    deserialize(message, data);

    for (String address : message.getBlacklistHosts()) {
      config.blacklistHost(address);
    }

    for (String address : message.getWhitelistHosts()) {
      config.whitelistHost(address);
    }

  }

  private HttpMethod build(Http1Method method) {
    if (method != null) {
      switch(method) {
      case CONNECT:
        return HttpMethod.CONNECT;
      case DELETE:
        return HttpMethod.DELETE;
      case GET:
        return HttpMethod.GET;
      case HEAD:
        return HttpMethod.HEAD;
      case OPTIONS:
        return HttpMethod.OPTIONS;
      case PATCH:
        return HttpMethod.PATCH;
      case POST:
        return HttpMethod.POST;
      case PUT:
        return HttpMethod.PUT;
      case TRACE:
        return HttpMethod.TRACE;
      }
    }
    return null;
  }

  private HttpVersion build(Http1Version version) {
    if (version != null) {
      switch(version) {
      case HTTP_1_0:
        return HttpVersion.HTTP_1_0;
      case HTTP_1_1:
        return HttpVersion.HTTP_1_1;
      }
    }
    return null;
  }

  private HashMultimap<String, String> build(List<Http1HeaderTuple> headers) {
    if (headers != null && headers.size() > 0) {
      HashMultimap<String, String> result = HashMultimap.create();

      for (Http1HeaderTuple tuple : headers) {
        result.put(tuple.getKey(), tuple.getValue());
      }

      return result;
    }
    return null;
  }

  private Http1DeterministicRuleEngineConfig.Rule build(Http1Rule rule) {
    return new Http1DeterministicRuleEngineConfig.Rule(
      build(rule.getMethod()),
      rule.getUri(),
      build(rule.getVersion()),
      build(rule.getHeaders())
    );
  }

  public void unmarshall(Http1DeterministicRuleEngineConfig config, byte[] data) {
    Http1Ruleset message = new Http1Ruleset();

    deserialize(message, data);

    for (Http1Rule rule : message.getBlacklistRules()) {
      config.blacklistRule(build(rule));
    }

    for (Http1Rule rule : message.getWhitelistRules()) {
      config.whitelistRule(build(rule));
    }

  }

  public void unmarshall(IpAddressDeterministicRuleEngineConfig config, byte[] data) {
    IpRuleset message = new IpRuleset();

    deserialize(message, data);

    try {
      for (ByteBuffer address : message.getBlacklistIps()) {
        config.blacklistIp(InetAddress.getByAddress(getBytes(address)));
      }

      for (ByteBuffer address : message.getWhitelistIps()) {
        config.whitelistIp(InetAddress.getByAddress(getBytes(address)));
      }
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }

  }
}
