package com.xjeffrose.xio.server;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XioServiceTest {

  @Test
  public void testConstructor() throws Exception {
    XioService xioService = new XioService();

    assertEquals(xioService, xioService.andThen(xioService));
  }

  @Test
  public void testChannelReadBlocking() {
    XioService xioService = new XioService();
    EmbeddedChannel channel = new EmbeddedChannel(xioService);
    String message = "Test Message";

    channel.writeInbound(message);

    assertEquals(message, channel.inboundMessages().poll());

    xioService.blockRead = true;

    channel.writeInbound(message);

    assertEquals(0, channel.inboundMessages().size());

  }
}
