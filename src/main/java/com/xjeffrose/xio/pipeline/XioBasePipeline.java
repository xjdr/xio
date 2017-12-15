package com.xjeffrose.xio.pipeline;

import static com.xjeffrose.xio.pipeline.Pipelines.addHandler;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.filter.Http1Filter;
import com.xjeffrose.xio.filter.IpFilter;
import com.xjeffrose.xio.server.XioBehavioralRuleEngine;
import com.xjeffrose.xio.server.XioConnectionLimiter;
import com.xjeffrose.xio.server.XioResponseClassifier;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerLimits;
import com.xjeffrose.xio.server.XioServerState;
import com.xjeffrose.xio.server.XioWebApplicationFirewall;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

abstract public class XioBasePipeline implements XioPipelineFragment {

  protected static final XioConnectionLimiter globalConnectionLimiter = new XioConnectionLimiter(15000);

  abstract public ChannelHandler getEncryptionHandler(XioServerConfig config, XioServerState state);

  abstract public ChannelHandler getAuthenticationHandler();

  abstract public ChannelHandler getAuthorizationHandler();

  abstract public ChannelHandler getCodecNegotiationHandler(XioServerConfig config);

  abstract public ChannelHandler getCodecHandler(XioServerConfig config);

  abstract public ChannelHandler getIdleDisconnectHandler(XioServerLimits limits);

  abstract public String applicationProtocol();

  public ChannelHandler getApplicationCodec() {
    return null;
  }

  public ChannelHandler getApplicationRouter() {
    return null;
  }

  abstract public ChannelHandler getApplicationHandler();

  public void buildHandlers(ApplicationState appState, XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
    // TODO(CK): pull globalConnectionLimiter from state
    pipeline.addLast("globalConnectionLimiter", globalConnectionLimiter); // TODO(JR): Need to make this config
    pipeline.addLast("serviceConnectionLimiter", new XioConnectionLimiter(config.getLimits().maxConnections()));
    ChannelHandler idleDisconnectHandler = getIdleDisconnectHandler(config.getLimits());
    pipeline.addLast("idleDisconnectHandler", idleDisconnectHandler);
    pipeline.addLast("l4DeterministicRuleEngine", new IpFilter(appState.getIpFilterConfig()));
    pipeline.addLast("l4BehavioralRuleEngine", new XioBehavioralRuleEngine(appState.getZkClient(), true)); // TODO(JR): Need to make this config
    pipeline.addLast("connectionContext", new ConnectionContextHandler());
    pipeline.addLast("globalChannelStatistics", state.getChannelStatistics());
    ChannelHandler encryptionHandler = getEncryptionHandler(config, state);
    if (encryptionHandler != null) {
      pipeline.addLast("encryptionHandler", encryptionHandler);
    }
    addHandler(pipeline, "authentication handler", getAuthenticationHandler());
    if (config.isMessageLoggerEnabled()) {
      pipeline.addLast("messageLogger", new XioMessageLogger(XioServer.class, config.getName()));
    }
    addHandler(pipeline, "codecNegotiation", getCodecNegotiationHandler(config));
    ChannelHandler codecHandler = getCodecHandler(config);
    if (codecHandler != null) {
      pipeline.addLast("codec", codecHandler);
    } else {
      throw new RuntimeException("No codec configured");
    }
    addHandler(pipeline, "distributed tracing", state.getTracingHandler().apply(false));
    addHandler(pipeline, "application codec", getApplicationCodec());
    addHandler(pipeline, "application router", getApplicationRouter());
    pipeline.addLast("l7DeterministicRuleEngine", new Http1Filter(appState.getHttp1FilterConfig()));
    pipeline.addLast("l7BehavioralRuleEngine", new XioBehavioralRuleEngine(appState.getZkClient(), true)); // TODO(JR): Need to make this config
    pipeline.addLast("webApplicationFirewall", new XioWebApplicationFirewall(appState.getZkClient(), true)); // TODO(JR): Need to make this config
    addHandler(pipeline, "authorization handler", getAuthorizationHandler());
    // See https://finagle.github.io/blog/2016/02/09/response-classification
    pipeline.addLast("xioResponseClassifier", new XioResponseClassifier(true)); /// TODO(JR): This is a maybe
    pipeline.addLast("exceptionLogger", new XioExceptionLogger());
    ChannelHandler applicationHandler = getApplicationHandler();
    if (applicationHandler != null) {
      pipeline.addLast("applicationHandler", applicationHandler);
    }
  }
}
