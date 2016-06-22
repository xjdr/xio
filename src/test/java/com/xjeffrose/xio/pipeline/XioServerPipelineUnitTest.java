package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.XioCodecFactory;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.server.XioBehavioralRuleEngine;
import com.xjeffrose.xio.server.XioConnectionLimiter;
import com.xjeffrose.xio.server.XioDeterministicRuleEngine;
import com.xjeffrose.xio.server.XioResponseClassifier;
import com.xjeffrose.xio.server.XioSecurityFactory;
import com.xjeffrose.xio.server.XioSecurityHandlers;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioService;
import com.xjeffrose.xio.server.XioWebApplicationFirewall;
import io.netty.channel.ChannelPipeline;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;
import org.junit.Test;

public class XioServerPipelineUnitTest {

  @Test
  public void verifyHandlers() {
    // Build pre-reqs
    XioServerDef def = mock(XioServerDef.class);
    XioServerConfig serverConfig = mock(XioServerConfig.class);
    XioSecurityFactory securityFactory = mock(XioSecurityFactory.class);
    when(def.getSecurityFactory()).thenReturn(securityFactory);
    XioSecurityHandlers securityHandlers = mock(XioSecurityHandlers.class);
    when(securityFactory.getSecurityHandlers(def, serverConfig)).thenReturn(securityHandlers);
    XioCodecFactory codecFactory = mock(XioCodecFactory.class);
    when(def.getCodecFactory()).thenReturn(codecFactory);
    ChannelStatistics channelStatistics = mock(ChannelStatistics.class);

    // Build class under test
    XioServerPipeline server = new XioServerPipeline(def, null, serverConfig, channelStatistics);
    ChannelPipeline pipeline = mock(ChannelPipeline.class);
    server.buildHandlers(pipeline);
    InOrder inOrder = inOrder(pipeline);
    inOrder.verify(pipeline, times(1)).addLast(eq("globalConnectionLimiter"), isA(XioConnectionLimiter.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("serviceConnectionLimiter"), isA(XioConnectionLimiter.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("l4DeterministicRuleEngine"), isA(XioDeterministicRuleEngine.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("l4BehavioralRuleEngine"), isA(XioBehavioralRuleEngine.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("connectionContext"), isA(ConnectionContextHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("globalChannelStatistics"), eq(channelStatistics));
    //inOrder.verify(pipeline, times(1)).addLast(eq("encryptionHandler"), isA(ChannelHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("encryptionHandler"), any());

    inOrder.verify(pipeline, times(1)).addLast(eq("messageLogger"), isA(XioMessageLogger.class));
    //inOrder.verify(pipeline, times(1)).addLast(eq("codec"), isA(XioMessageLogger.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("codec"), any());
    inOrder.verify(pipeline, times(1)).addLast(eq("l7DeterministicRuleEngine"), isA(XioDeterministicRuleEngine.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("l7BehavioralRuleEngine"), isA(XioBehavioralRuleEngine.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("webApplicationFirewall"), isA(XioWebApplicationFirewall.class));
    //inOrder.verify(pipeline, times(1)).addLast(eq("authHandler"), isA(ChannelHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("authHandler"), any());
    inOrder.verify(pipeline, times(1)).addLast(eq("xioService"), isA(XioService.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("xioResponseClassifier"), isA(XioResponseClassifier.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("exceptionLogger"), isA(XioExceptionLogger.class));
  }
}
