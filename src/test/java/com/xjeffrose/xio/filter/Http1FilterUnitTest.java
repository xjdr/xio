package com.xjeffrose.xio.filter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.config.Http1DeterministicRuleEngineConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Http1FilterUnitTest extends Assert {

  @Test
  public void testDeniedRule() throws UnknownHostException {
    List<Http1DeterministicRuleEngineConfig.Rule> blacklist = new ArrayList<>();
    HashMultimap<String, String> headers = HashMultimap.create();
    headers.put("User-Agent", "Bad-actor: 1.0");
    Http1DeterministicRuleEngineConfig.Rule bad = new Http1DeterministicRuleEngineConfig.Rule(
      HttpMethod.GET,
      "/path/to/failure",
      HttpVersion.HTTP_1_0,
      headers
    );
    blacklist.add(bad);
    Http1Filter http1Filter = new Http1Filter(new Http1FilterConfig(ImmutableList.copyOf(blacklist)));
    EmbeddedChannel chDeny = new EmbeddedChannel(http1Filter);
    DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/path/to/failure");
    request.headers().set("User-Agent", "Bad-actor: 1.0");
    chDeny.writeInbound(request);
    chDeny.runPendingTasks();
    assertFalse(chDeny.isActive());
    assertFalse(chDeny.isOpen());
  }

  @Test
  public void testAllowedRule() throws UnknownHostException {
    List<Http1DeterministicRuleEngineConfig.Rule> blacklist = new ArrayList<>();
    HashMultimap<String, String> headers = HashMultimap.create();
    headers.put("User-Agent", "Bad-actor: 1.0");
    Http1DeterministicRuleEngineConfig.Rule bad = new Http1DeterministicRuleEngineConfig.Rule(
      HttpMethod.POST,
      "/path/to/failure",
      HttpVersion.HTTP_1_1,
      headers
    );
    blacklist.add(bad);
    Http1Filter http1Filter = new Http1Filter(new Http1FilterConfig(ImmutableList.copyOf(blacklist)));
    EmbeddedChannel chAllow = new EmbeddedChannel(http1Filter);
    DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "/path/to/failure");
    request.headers().set("User-Agent", "Bad-actor: 1.0");
    chAllow.writeInbound(request);

    assertTrue(chAllow.isActive());
    assertTrue(chAllow.isOpen());
  }

}
