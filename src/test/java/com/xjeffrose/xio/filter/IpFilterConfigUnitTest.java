package com.xjeffrose.xio.filter;

import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.config.IpAddressDeterministicRuleEngineConfig;
import com.xjeffrose.xio.marshall.ThriftMarshaller;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class IpFilterConfigUnitTest extends Assert {

  IpFilterConfig config;

  @Test
  public void testUpdater() throws UnknownHostException {
    ThriftMarshaller marshaller = new ThriftMarshaller();
    IpAddressDeterministicRuleEngineConfig rules = new IpAddressDeterministicRuleEngineConfig();

    rules.blacklistIp(InetAddress.getByName("127.0.0.1"));

    IpFilterConfig.Updater updater = new IpFilterConfig.Updater("path", this::setIpFilterConfig);
    updater.update(marshaller.marshall(rules));
    IpFilterConfig expected = new IpFilterConfig(rules.getBlacklistIps());

    assertEquals(expected, config);
  }

  public void setIpFilterConfig(IpFilterConfig config) {
    this.config = config;
  }

}
