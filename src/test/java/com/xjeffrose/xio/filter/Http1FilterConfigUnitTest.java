package com.xjeffrose.xio.filter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.config.Http1DeterministicRuleEngineConfig;
import com.xjeffrose.xio.config.IpAddressDeterministicRuleEngineConfig;
import com.xjeffrose.xio.marshall.ThriftMarshaller;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class Http1FilterConfigUnitTest extends Assert {

  Http1FilterConfig config;

  @Test
  public void testUpdater() throws UnknownHostException {
    ThriftMarshaller marshaller = new ThriftMarshaller();

    Http1DeterministicRuleEngineConfig rules = new Http1DeterministicRuleEngineConfig();

    HashMultimap<String, String> headers = HashMultimap.create();
    headers.put("User-Agent", "Bad-actor: 1.0");
    Http1DeterministicRuleEngineConfig.Rule bad = new Http1DeterministicRuleEngineConfig.Rule(
      HttpMethod.GET,
      "/path/to/failure",
      HttpVersion.HTTP_1_0,
      headers
    );

    rules.blacklistRule(bad);

    Http1FilterConfig.Updater updater = new Http1FilterConfig.Updater("path", this::setHttp1FilterConfig);
    updater.update(marshaller.marshall(rules));
    Http1FilterConfig expected = new Http1FilterConfig(rules.getBlacklistRules());

    assertEquals(expected, config);
  }

  public void setHttp1FilterConfig(Http1FilterConfig config) {
    this.config = config;
  }

}
