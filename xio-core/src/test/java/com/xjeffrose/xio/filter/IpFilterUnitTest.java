package com.xjeffrose.xio.filter;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.xjeffrose.xio.firewall.BlackListFilter;
import com.xjeffrose.xio.firewall.Firewall;
import com.xjeffrose.xio.firewall.WhiteListFilter;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import org.junit.Assert;
import org.junit.Test;

public class IpFilterUnitTest extends Assert {

  @Test
  public void testBlackListedIpAllow() throws UnknownHostException {
    // given a black listed ip
    ImmutableSet<InetAddress> blackList =
        ImmutableSet.<InetAddress>builder().add(InetAddress.getByName("172.22.10.1")).build();
    ImmutableSet<InetAddress> whiteList = ImmutableSet.of();

    // and given a black list filter and firewall in the pipeline
    IpFilterConfig ipFilterConfig = new IpFilterConfig(blackList, whiteList);
    BlackListFilter ipFilter = new BlackListFilter(ipFilterConfig);
    Firewall firewall = new Firewall(new MetricRegistry());

    // when the an ip connects that is NOT black listed
    MyEmbeddedChannel channel = new MyEmbeddedChannel("172.22.10.5", ipFilter, firewall);
    channel.runPendingTasks();

    // then the channel is NOT closed
    assertTrue(channel.isActive());
    assertTrue(channel.isOpen());
  }

  @Test
  public void testBlackListedIpDeny() throws UnknownHostException {
    // given a black listed ip
    ImmutableSet<InetAddress> blackList =
        ImmutableSet.<InetAddress>builder().add(InetAddress.getByName("172.22.10.1")).build();
    ImmutableSet<InetAddress> whiteList = ImmutableSet.of();

    // and given a black list filter and firewall in the pipeline
    IpFilterConfig ipFilterConfig = new IpFilterConfig(blackList, whiteList);
    BlackListFilter ipFilter = new BlackListFilter(ipFilterConfig);
    Firewall firewall = new Firewall(new MetricRegistry());

    // when the black listed ip connects
    MyEmbeddedChannel channel = new MyEmbeddedChannel("172.22.10.1", ipFilter, firewall);
    channel.runPendingTasks();

    // then the channel is closed
    assertFalse(channel.isActive());
    assertFalse(channel.isOpen());
  }

  @Test
  public void testWhiteListedIpAllow() throws UnknownHostException {
    // given a white listed ip
    ImmutableSet<InetAddress> whiteList =
        ImmutableSet.<InetAddress>builder().add(InetAddress.getByName("172.22.10.1")).build();
    ImmutableSet<InetAddress> blackList = ImmutableSet.of();

    // and given a white list filter and firewall in the pipeline
    IpFilterConfig ipFilterConfig = new IpFilterConfig(blackList, whiteList);
    WhiteListFilter ipFilter = new WhiteListFilter(ipFilterConfig);
    Firewall firewall = new Firewall(new MetricRegistry());

    // when the white listed ip connects
    MyEmbeddedChannel channel = new MyEmbeddedChannel("172.22.10.1", ipFilter, firewall);
    channel.runPendingTasks();

    // then the channel is NOT closed
    assertTrue(channel.isActive());
    assertTrue(channel.isOpen());
  }

  @Test
  public void testWhiteListedIpDeny() throws UnknownHostException {
    // given a white listed ip
    ImmutableSet<InetAddress> whiteList =
        ImmutableSet.<InetAddress>builder().add(InetAddress.getByName("172.22.10.1")).build();
    ImmutableSet<InetAddress> blackList = ImmutableSet.of();

    // and given a white list filter and firewall in the pipeline
    IpFilterConfig ipFilterConfig = new IpFilterConfig(blackList, whiteList);
    WhiteListFilter ipFilter = new WhiteListFilter(ipFilterConfig);
    Firewall firewall = new Firewall(new MetricRegistry());

    // when the an ip connects that is NOT white-listed
    MyEmbeddedChannel chDeny = new MyEmbeddedChannel("172.22.10.5", ipFilter, firewall);
    chDeny.runPendingTasks();

    // then the channel is closed
    assertFalse(chDeny.isActive());
    assertFalse(chDeny.isOpen());
  }

  private static class MyEmbeddedChannel extends EmbeddedChannel {
    private final InetSocketAddress ipAddress;

    private MyEmbeddedChannel(String ipAddress, ChannelHandler... handlers) {
      super(false, true, handlers);
      this.ipAddress = new InetSocketAddress(ipAddress, 5421);
      try {
        register();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    @Override
    protected SocketAddress remoteAddress0() {
      return ipAddress;
    }
  }
}
