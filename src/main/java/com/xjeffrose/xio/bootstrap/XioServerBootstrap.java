package com.xjeffrose.xio.bootstrap;

import com.xjeffrose.xio.pipeline.XioPipelineAssembler;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerEndpoint;
import com.xjeffrose.xio.server.XioServerInstrumentation;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
public class XioServerBootstrap {
  private static final Logger log = LoggerFactory.getLogger(XioServerBootstrap.class);

  private final ServerBootstrap serverBootstrap;

  private final XioPipelineAssembler pipelineAssembler;

  private XioServerEndpoint endpoint;

  private ChannelConfiguration channelConfig;

  public XioServerBootstrap(XioServerConfig config) {
    serverBootstrap = new ServerBootstrap();
    pipelineAssembler = new XioPipelineAssembler(config);
  }

  public XioServerBootstrap addToPipeline(XioPipelineFragment fragment) {
    pipelineAssembler.addFragment(fragment);
    return this;
  }

  public XioServerBootstrap channelConfig(ChannelConfiguration channelConfig) {
    this.channelConfig = channelConfig;
    return this;
  }

  public XioServerBootstrap endpoint(XioServerEndpoint endpoint) {
    this.endpoint = endpoint;
    serverBootstrap.localAddress(endpoint.bindAddress());
    return this;
  }

  public XioServer build() {
    log.debug("Building");
    serverBootstrap.group(channelConfig.bossGroup(), channelConfig.workerGroup());
    serverBootstrap.channel(channelConfig.channel());
    final XioServerInstrumentation instrumentation = new XioServerInstrumentation();
    serverBootstrap.childHandler(pipelineAssembler.build(instrumentation));
    ChannelFuture future = serverBootstrap.bind();
    endpoint.afterBind(future); // TODO(CK): kill this
    future.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) {
        if (future.isSuccess()) {
          instrumentation.addressBound = (InetSocketAddress)future.channel().localAddress();
        }
      }
    });
    future.awaitUninterruptibly();
    return new XioServer(future.channel(), instrumentation);
  }
}
