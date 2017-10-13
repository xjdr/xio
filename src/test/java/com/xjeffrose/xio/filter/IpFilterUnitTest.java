package com.xjeffrose.xio.filter;

import com.google.common.collect.ImmutableSet;
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

public class IpFilterUnitTest extends Assert {

  boolean active = false;
  boolean registered = false;
  ChannelHandler eventTracker = new ChannelInboundHandlerAdapter() {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      setActive();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
      setRegistered();
    }
  };

  void setActive() {
    active = true;
  }

  void setRegistered() {
    registered = true;
  }

  @Test
  public void testEagerDeniedIp() throws UnknownHostException {
    Set<InetAddress> blacklist = new HashSet<InetAddress>();
    blacklist.add(InetAddress.getByName("172.22.10.1"));
    IpFilter ipFilter = new IpFilter(new IpFilterConfig(ImmutableSet.copyOf(blacklist)));
    EmbeddedChannel chDeny = newEmbeddedInetChannel("172.22.10.1", true, ipFilter, eventTracker);
    chDeny.runPendingTasks();
    assertFalse(chDeny.isActive());
    assertFalse(chDeny.isOpen());
    assertFalse(active);
    // AD: Eager Denial in netty 4.1.14: after closing context, prevents next handler from registering.
    assertFalse(registered);
  }

  @Test
  public void testDeniedIp() throws UnknownHostException {
    Set<InetAddress> blacklist = new HashSet<InetAddress>();
    blacklist.add(InetAddress.getByName("172.22.10.1"));
    IpFilter ipFilter = new IpFilter(new IpFilterConfig(ImmutableSet.copyOf(blacklist)));
    EmbeddedChannel chDeny = newEmbeddedInetChannel("172.22.10.1", false, ipFilter, eventTracker);
    assertFalse(chDeny.isActive());
    assertFalse(chDeny.isOpen());
    // AD: Denial in netty 4.1.14: after closing context, prevents next handler from activating.
    assertFalse(active);
    assertTrue(registered);
  }

  @Test
  public void testAllowedIp() throws UnknownHostException {
    Set<InetAddress> blacklist = new HashSet<InetAddress>();
    blacklist.add(InetAddress.getByName("172.22.10.1"));
    IpFilter ipFilter = new IpFilter(new IpFilterConfig(ImmutableSet.copyOf(blacklist)));
    EmbeddedChannel chAllow = newEmbeddedInetChannel("172.22.10.2", true, ipFilter, eventTracker);
    assertTrue(chAllow.isActive());
    assertTrue(chAllow.isOpen());
    assertTrue(active);
    assertTrue(registered);
  }

  private EmbeddedChannel newEmbeddedInetChannel(final String ipAddress, boolean issueAddress, ChannelHandler... handlers) {
    return new EmbeddedChannel(handlers) {
      @Override
      protected SocketAddress remoteAddress0() {
        InetSocketAddress address = new InetSocketAddress(ipAddress, 5421);

        if (issueAddress) {
          return address;
        } else if (registered) {
          return address;
        }
        return null;
      }
    };
  }
}
