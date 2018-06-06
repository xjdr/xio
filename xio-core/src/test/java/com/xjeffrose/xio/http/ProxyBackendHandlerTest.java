package com.xjeffrose.xio.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class ProxyBackendHandlerTest extends Assert {

  @Mock ChannelHandlerContext frontEndCtx;
  @Mock ChannelHandlerContext clientCtx;
  private ProxyBackendHandler subject;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    subject = new ProxyBackendHandler(frontEndCtx);
  }

  @Test
  public void testDisconnectWhenClientIsIdle() throws Exception {
    subject.channelInactive(clientCtx);
    verify(frontEndCtx).fireUserEventTriggered(any());
  }
}
