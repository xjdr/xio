package com.xjeffrose.xio.pipeline;

import static com.xjeffrose.xio.pipeline.Pipelines.addHandler;

import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.filter.Http1Filter;
import com.xjeffrose.xio.firewall.*;
import com.xjeffrose.xio.metric.MetricsHandler;
import com.xjeffrose.xio.server.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

public abstract class XioBasePipeline implements XioPipelineFragment {

  public abstract ChannelHandler getEncryptionHandler(XioServerConfig config, XioServerState state);

  public ChannelHandler getTlsAuthenticationHandler() {
    return null;
  }

  public abstract ChannelHandler getAuthenticationHandler();

  public abstract ChannelHandler getAuthorizationHandler();

  public abstract ChannelHandler getCodecNegotiationHandler(XioServerConfig config);

  public abstract ChannelHandler getCodecHandler(XioServerConfig config);

  public abstract ChannelHandler getIdleDisconnectHandler(XioServerLimits limits);

  public abstract String applicationProtocol();

  public ChannelHandler getApplicationCodec(XioServerConfig config) {
    return null;
  }

  public ChannelHandler getApplicationRouter() {
    return null;
  }

  public abstract ChannelHandler getApplicationHandler();

  public void buildHandlers(
      ApplicationState appState,
      XioServerConfig config,
      XioServerState state,
      ChannelPipeline pipeline) {
    ChannelHandler idleDisconnectHandler = getIdleDisconnectHandler(config.getLimits());
    pipeline.addLast("idleDisconnectHandler", idleDisconnectHandler);
    pipeline.addLast(
        "l4ConnectionLimiter",
        new ConnectionLimiter(appState.getMetricRegistry(), config.getLimits()));
    //todo: (WK) ServiceRateLimiter needs to be tested b4 we allow it to be added to the pipeline
    //pipeline.addLast(
    //    "l4RateLimiter", new ServiceRateLimiter(appState.getMetricRegistry(), config.getLimits()));
    if (config.isWhiteListEnabled()) {
      pipeline.addLast("l4WhiteListFilter", new WhiteListFilter(appState.getIpFilterConfig()));
    } else if (config.isBlackListEnabled()) {
      pipeline.addLast("l4BlackListFilter", new BlackListFilter(appState.getIpFilterConfig()));
    }
    pipeline.addLast("l4Firewall", new Firewall(appState.getMetricRegistry()));
    pipeline.addLast("connectionContext", new ConnectionContextHandler());
    pipeline.addLast("globalChannelStatistics", state.getChannelStatistics());
    ChannelHandler encryptionHandler = getEncryptionHandler(config, state);
    addHandler(pipeline, "encryptionHandler", encryptionHandler);
    addHandler(pipeline, "tls authentication handler", getTlsAuthenticationHandler());
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
    addHandler(pipeline, "application codec", getApplicationCodec(config));
    addHandler(pipeline, "metric handler", new MetricsHandler(appState.getMetricRegistry()));
    addHandler(pipeline, "distributed tracing", state.tracingHandler(appState));
    addHandler(pipeline, "application router", getApplicationRouter());
    addHandler(pipeline, "authentication handler", getAuthenticationHandler());
    pipeline.addLast("l7DeterministicRuleEngine", new Http1Filter(appState.getHttp1FilterConfig()));
    addHandler(pipeline, "authorization handler", getAuthorizationHandler());
    // See https://finagle.github.io/blog/2016/02/09/response-classification
    pipeline.addLast(
        "xioResponseClassifier", new XioResponseClassifier(true)); // / TODO(JR): This is a maybe
    pipeline.addLast("exceptionLogger", new XioExceptionLogger());
    ChannelHandler applicationHandler = getApplicationHandler();
    addHandler(pipeline, "applicationHandler", applicationHandler);
  }
}
