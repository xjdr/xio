package com.xjeffrose.xio.pipeline;

import static org.mockito.Mockito.*;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.core.XioNoOpHandler;
import com.xjeffrose.xio.filter.Http1Filter;
import com.xjeffrose.xio.firewall.ConnectionLimiter;
import com.xjeffrose.xio.firewall.Firewall;
import com.xjeffrose.xio.server.*;
import com.xjeffrose.xio.tracing.HttpServerTracingHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import org.junit.Test;
import org.mockito.InOrder;

public class XioServerPipelineUnitTest {

  @Test
  public void verifyHandlers() {
    Config config = ConfigFactory.load();
    ApplicationState appState =
        new ApplicationState(ApplicationConfig.fromConfig("xio.testApplication", config));
    XioServerConfig serverConfig =
        XioServerConfig.fromConfig("xio.testApplication.servers.testServer");
    XioServerState serverState = new XioServerState(serverConfig);

    // Build class under test
    XioServerPipeline server =
        new XioServerPipeline() {
          @Override
          public ChannelHandler getEncryptionHandler(XioServerConfig config, XioServerState state) {
            return new XioNoOpHandler();
          }

          @Override
          public ChannelHandler getIdleDisconnectHandler(XioServerLimits limits) {
            return new XioNoOpHandler();
          }

          @Override
          public ChannelHandler getTlsAuthenticationHandler() {
            return new XioNoOpHandler();
          }

          @Override
          public ChannelHandler getAuthenticationHandler() {
            return new XioNoOpHandler();
          }

          @Override
          public ChannelHandler getAuthorizationHandler() {
            return new XioNoOpHandler();
          }

          @Override
          public ChannelHandler getCodecNegotiationHandler(XioServerConfig config) {
            return new XioNoOpHandler();
          }

          @Override
          public ChannelHandler getCodecHandler(XioServerConfig config) {
            return new XioNoOpHandler();
          }

          @Override
          public ChannelHandler getApplicationCodec(XioServerConfig config) {
            return new XioNoOpHandler();
          }

          @Override
          public ChannelHandler getApplicationRouter() {
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
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("idleDisconnectHandler"), isA(XioNoOpHandler.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("l4ConnectionLimiter"), isA(ConnectionLimiter.class));
    //todo: (WK) ServiceRateLimiter needs to be tested b4 we allow it to be added to the pipeline
    //inOrder.verify(pipeline, times(1)).addLast(eq("l4RateLimiter"), isA(ServiceRateLimiter.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("l4Firewall"), isA(Firewall.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("connectionContext"), isA(ConnectionContextHandler.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("globalChannelStatistics"), eq(serverState.getChannelStatistics()));
    inOrder.verify(pipeline, times(1)).addLast(eq("encryptionHandler"), isA(XioNoOpHandler.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("tls authentication handler"), isA(XioNoOpHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("messageLogger"), isA(XioMessageLogger.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("codecNegotiation"), isA(XioNoOpHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("codec"), isA(XioNoOpHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("application codec"), isA(XioNoOpHandler.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("distributed tracing"), isA(HttpServerTracingHandler.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("application router"), isA(XioNoOpHandler.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("authentication handler"), isA(XioNoOpHandler.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("l7DeterministicRuleEngine"), isA(Http1Filter.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("authorization handler"), isA(XioNoOpHandler.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("xioResponseClassifier"), isA(XioResponseClassifier.class));
    inOrder
        .verify(pipeline, times(1))
        .addLast(eq("exceptionLogger"), isA(XioExceptionLogger.class));
    inOrder.verify(pipeline, times(1)).addLast(eq("applicationHandler"), isA(XioNoOpHandler.class));
  }
}
