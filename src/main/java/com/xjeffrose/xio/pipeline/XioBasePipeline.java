package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.core.ChannelStatistics;
import com.xjeffrose.xio.core.ConnectionContextHandler;
import com.xjeffrose.xio.core.XioExceptionLogger;
import com.xjeffrose.xio.core.XioMessageLogger;
import com.xjeffrose.xio.core.ZkClient;
import com.xjeffrose.xio.server.XioBehavioralRuleEngine;
import com.xjeffrose.xio.server.XioConnectionLimiter;
import com.xjeffrose.xio.server.XioDeterministicRuleEngine;
import com.xjeffrose.xio.server.XioResponseClassifier;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioService;
import com.xjeffrose.xio.server.XioWebApplicationFirewall;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;

abstract public class XioBasePipeline implements XioPipelineFragment {

  protected final XioServerDef def;
  protected final ZkClient zkClient;
  protected final XioServerConfig xioServerConfig;
  protected final ChannelStatistics channelStatistics;
  protected static final XioConnectionLimiter globalConnectionLimiter = new XioConnectionLimiter(15000);

  public XioBasePipeline(XioServerDef def, ZkClient zkClient, XioServerConfig xioServerConfig, ChannelStatistics channelStatistics) {
    this.def = def;
    this.zkClient = zkClient;
    this.xioServerConfig = xioServerConfig;
    this.channelStatistics = channelStatistics;
  }

  abstract public ChannelHandler getEncryptionHandler();

  abstract public ChannelHandler getAuthenticationHandler();

  abstract public void addIdleDisconnectHandler(ChannelPipeline pipeline);

  public void buildHandlers(ChannelPipeline pipeline) {
    pipeline.addLast("globalConnectionLimiter", globalConnectionLimiter); // TODO(JR): Need to make this config
    pipeline.addLast("serviceConnectionLimiter", new XioConnectionLimiter(def.getMaxConnections()));
    pipeline.addLast("l4DeterministicRuleEngine", new XioDeterministicRuleEngine(zkClient, true)); // TODO(JR): Need to make this config
    pipeline.addLast("l4BehavioralRuleEngine", new XioBehavioralRuleEngine(zkClient, true)); // TODO(JR): Need to make this config
    pipeline.addLast("connectionContext", new ConnectionContextHandler());
    pipeline.addLast("globalChannelStatistics", channelStatistics);
    pipeline.addLast("encryptionHandler", getEncryptionHandler());
    pipeline.addLast("messageLogger", new XioMessageLogger()); // THIS IS FOR DEBUG ONLY AND SHOULD BE REMOVED OTHERWISE
    pipeline.addLast("codec", def.getCodecFactory().getCodec());
    pipeline.addLast("l7DeterministicRuleEngine", new XioDeterministicRuleEngine(zkClient, true)); // TODO(JR): Need to make this config
    pipeline.addLast("l7BehavioralRuleEngine", new XioBehavioralRuleEngine(zkClient, true)); // TODO(JR): Need to make this config
    pipeline.addLast("webApplicationFirewall", new XioWebApplicationFirewall(zkClient, true)); // TODO(JR): Need to make this config
    pipeline.addLast("authHandler", getAuthenticationHandler());
    pipeline.addLast("xioService", new XioService());
    addIdleDisconnectHandler(pipeline);
    // See https://finagle.github.io/blog/2016/02/09/response-classification
    pipeline.addLast("xioResponseClassifier", new XioResponseClassifier(true)); /// TODO(JR): This is a maybe
    pipeline.addLast("exceptionLogger", new XioExceptionLogger());
  }
}
