package com.xjeffrose.xio.http;

import com.xjeffrose.xio.core.XioIdleDisconnectHandler;
import com.xjeffrose.xio.http.internal.ProxyClientIdle;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class XioIdleDisconnectHandlerTest extends Assert {

  private XioIdleDisconnectHandler subject;
  private EmbeddedChannel channel;

  @Before
  public void setUp() {
    subject = new XioIdleDisconnectHandler(1, 1, 1);
    channel = new EmbeddedChannel();
    channel.pipeline().addLast(subject);
  }

  @Test
  public void testReceivedClientIdleUserEvent() {
    channel.pipeline().fireUserEventTriggered(ProxyClientIdle.INSTANCE);
    assertFalse(channel.isOpen());
  }
}
