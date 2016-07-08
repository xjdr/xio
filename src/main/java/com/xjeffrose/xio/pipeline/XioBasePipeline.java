package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.server.XioBehavioralRuleEngine;
import com.xjeffrose.xio.server.XioConnectionLimiter;
import com.xjeffrose.xio.server.XioDeterministicRuleEngine;
import com.xjeffrose.xio.server.XioResponseClassifier;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerState;
import com.xjeffrose.xio.server.XioService;
import com.xjeffrose.xio.server.XioWebApplicationFirewall;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

abstract public class XioBasePipeline implements XioPipelineFragment {

  protected final XioServerDef def;
  protected static final XioConnectionLimiter globalConnectionLimiter = new XioConnectionLimiter(15000);

  public XioBasePipeline() {
    def = null;
  }

  public XioBasePipeline(XioServerDef def) {
    this.def = def;
  }

  // TODO(CK): change these from get to add, similar to addIdleDisconnectHandler
  abstract public ChannelHandler getEncryptionHandler();

  abstract public ChannelHandler getAuthenticationHandler();

  abstract public ChannelHandler getCodecHandler();

  abstract public void addIdleDisconnectHandler(ChannelPipeline pipeline);

  abstract public String applicationProtocol();

  public void buildHandlers(XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
    // TODO(CK): pull globalConnectionLimiter from state
    pipeline.addLast("globalConnectionLimiter", globalConnectionLimiter); // TODO(JR): Need to make this config
    pipeline.addLast("serviceConnectionLimiter", new XioConnectionLimiter(def.getMaxConnections()));
    pipeline.addLast("l4DeterministicRuleEngine", new XioDeterministicRuleEngine(state.zkClient(), true)); // TODO(JR): Need to make this config
    pipeline.addLast("l4BehavioralRuleEngine", new XioBehavioralRuleEngine(state.zkClient(), true)); // TODO(JR): Need to make this config
    pipeline.addLast("connectionContext", new ConnectionContextHandler());
    pipeline.addLast("globalChannelStatistics", state.channelStatistics());
    pipeline.addLast("encryptionHandler", getEncryptionHandler());
    pipeline.addLast("messageLogger", new XioMessageLogger()); // THIS IS FOR DEBUG ONLY AND SHOULD BE REMOVED OTHERWISE
    pipeline.addLast("codec", getCodecHandler());
    pipeline.addLast("l7DeterministicRuleEngine", new XioDeterministicRuleEngine(state.zkClient(), true)); // TODO(JR): Need to make this config
    pipeline.addLast("l7BehavioralRuleEngine", new XioBehavioralRuleEngine(state.zkClient(), true)); // TODO(JR): Need to make this config
    pipeline.addLast("webApplicationFirewall", new XioWebApplicationFirewall(state.zkClient(), true)); // TODO(JR): Need to make this config
    pipeline.addLast("authHandler", getAuthenticationHandler());
    pipeline.addLast("xioService", new XioService());
    addIdleDisconnectHandler(pipeline);
    // See https://finagle.github.io/blog/2016/02/09/response-classification
    pipeline.addLast("xioResponseClassifier", new XioResponseClassifier(true)); /// TODO(JR): This is a maybe
    pipeline.addLast("exceptionLogger", new XioExceptionLogger());
  }
}
