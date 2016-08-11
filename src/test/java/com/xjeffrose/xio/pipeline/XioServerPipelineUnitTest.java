package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.core.XioNoOpHandler;
import com.xjeffrose.xio.server.XioBehavioralRuleEngine;
import com.xjeffrose.xio.server.XioConnectionLimiter;
import com.xjeffrose.xio.server.XioDeterministicRuleEngine;
import com.xjeffrose.xio.server.XioResponseClassifier;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import com.xjeffrose.xio.server.XioService;
import com.xjeffrose.xio.server.XioWebApplicationFirewall;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;
import org.junit.Test;

public class XioServerPipelineUnitTest {

  @Test
  public void verifyHandlers() {
    ApplicationState appState = ApplicationState.fromConfig("xio.testApplication");
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.testApplication.servers.testServer");
    XioServerState serverState = XioServerState.fromConfig("xio.testApplication.servers.testServer");

    // Build class under test
    XioServerPipeline server = new XioServerPipeline() {
      @Override
      public ChannelHandler getEncryptionHandler(XioServerConfig config, XioServerState state) {
        return new XioNoOpHandler();
      }

      @Override
      public ChannelHandler getAuthenticationHandler() {
        return new XioNoOpHandler();
      }

      @Override
      public ChannelHandler getCodecHandler() {
        return new XioNoOpHandler();
      }

      @Override
      public ChannelHandler getApplicationHandler() {
        return new XioNoOpHandler();
      }
    };
    ChannelPipeline pipeline = mock(ChannelPipeline.class);
    server.buildHandlers(appState, serverConfig, serverState, pipeline);
    InOrder inOrder = inOrder(pipeline);
    inOrder.verify(pipeline, times(1)).addLast(eq("globalConnectionLimiter"), isA(XioConnectionLimiter.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("serviceConnectionLimiter"), isA(XioConnectionLimiter.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("l4DeterministicRuleEngine"), isA(XioDeterministicRuleEngine.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("l4BehavioralRuleEngine"), isA(XioBehavioralRuleEngine.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("connectionContext"), isA(ConnectionContextHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("globalChannelStatistics"), eq(serverState.getChannelStatistics()));
    inOrder.verify(pipeline, times(1)).addLast(eq("encryptionHandler"), isA(XioNoOpHandler.class));

    inOrder.verify(pipeline, times(1)).addLast(eq("messageLogger"), isA(XioMessageLogger.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("codec"), isA(XioNoOpHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("l7DeterministicRuleEngine"), isA(XioDeterministicRuleEngine.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("l7BehavioralRuleEngine"), isA(XioBehavioralRuleEngine.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("webApplicationFirewall"), isA(XioWebApplicationFirewall.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("authHandler"), isA(XioNoOpHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("xioService"), isA(XioService.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("xioResponseClassifier"), isA(XioResponseClassifier.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("exceptionLogger"), isA(XioExceptionLogger.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("applicationHandler"), isA(XioNoOpHandler.class));
  }
}
