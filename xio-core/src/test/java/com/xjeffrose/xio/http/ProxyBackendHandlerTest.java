package com.xjeffrose.xio.http;

import static org.mockito.Mockito.verify;

import com.xjeffrose.xio.http.internal.ProxyClientIdle;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    verify(frontEndCtx).fireUserEventTriggered(ProxyClientIdle.INSTANCE);
  }
}
