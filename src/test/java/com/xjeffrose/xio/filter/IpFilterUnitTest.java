package com.xjeffrose.xio.filter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.common.collect.ImmutableSet;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IpFilter.class, LoggerFactory.class})
public class IpFilterUnitTest extends Assert {

  private boolean eager;

  @Before
  public void setUp() throws Exception {
    eager = true;
    // TODO(CK): This is a bit kludgy, basically we create a new logger for every test
    // but log is a static member on ipFilter, we should probably just emit events instead
    // of logging.
    mockStatic(LoggerFactory.class);
    Logger logger = mock(Logger.class);
    when(LoggerFactory.getLogger(any(Class.class))).thenReturn(logger);
  }

  @Test
  public void testEagerDeniedIp() throws UnknownHostException {
    Set<InetAddress> blacklist = new HashSet<InetAddress>();
    blacklist.add(InetAddress.getByName("172.22.10.1"));
    IpFilter ipFilter = new IpFilter(new IpFilterConfig(ImmutableSet.copyOf(blacklist)));
    EmbeddedChannel chDeny = newEmbeddedInetChannel("172.22.10.1", true, ipFilter);
    chDeny.runPendingTasks();
    assertFalse(chDeny.isActive());
    assertFalse(chDeny.isOpen());
    verify(ipFilter.getLog()).warn("IpFilter denied blacklisted ip '{}'{}", "172.22.10.1", " (eager)");
  }

  @Test
  public void testDeniedIp() throws UnknownHostException {
    Set<InetAddress> blacklist = new HashSet<InetAddress>();
    blacklist.add(InetAddress.getByName("172.22.10.1"));
    IpFilter ipFilter = new IpFilter(new IpFilterConfig(ImmutableSet.copyOf(blacklist)));
    EmbeddedChannel chDeny = newEmbeddedInetChannel("172.22.10.1", false, ipFilter);
    chDeny.runPendingTasks();
    assertFalse(chDeny.isActive());
    assertFalse(chDeny.isOpen());
    verify(ipFilter.getLog()).warn("IpFilter denied blacklisted ip '{}'{}", "172.22.10.1", "");
  }

  @Test
  public void testAllowedIp() throws UnknownHostException {
    Set<InetAddress> blacklist = new HashSet<InetAddress>();
    blacklist.add(InetAddress.getByName("172.22.10.1"));
    IpFilter ipFilter = new IpFilter(new IpFilterConfig(ImmutableSet.copyOf(blacklist)));
    EmbeddedChannel chAllow = newEmbeddedInetChannel("172.22.10.2", true, ipFilter);
    chAllow.runPendingTasks();
    assertTrue(chAllow.isActive());
    assertTrue(chAllow.isOpen());
    verify(ipFilter.getLog()).info("IpFilter allowed ip '{}'", "172.22.10.2");
  }


  private EmbeddedChannel newEmbeddedInetChannel(final String ipAddress, boolean issueAddress, ChannelHandler... handlers) {
    return new EmbeddedChannel(handlers) {

      @Override
      protected SocketAddress remoteAddress0() {
        InetSocketAddress address = new InetSocketAddress(ipAddress, 5421);

        if (eager && !issueAddress) {
          // this is channelRegistered and we don't want to issue an address
          eager = false;
          return null;
        } else if (eager && issueAddress) {
          // this is channelRegistered and we want to issue an address
          eager = false;
          return address;
        } else if (super.isActive()) {
          // this is channelActive
          return address;
        }
        return null;
      }
    };
  }
}
