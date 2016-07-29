package com.xjeffrose.xio.bootstrap;

import com.xjeffrose.xio.pipeline.XioPipelineAssembler;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerInstrumentation;
import com.xjeffrose.xio.server.XioServerState;
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

  private ChannelConfiguration channelConfig;

  public XioServerBootstrap(XioServerConfig config, XioServerState state) {
    serverBootstrap = new ServerBootstrap();
    pipelineAssembler = new XioPipelineAssembler(config, state);
    bindAddress(config.getBindAddress());
  }

  public XioServerBootstrap addToPipeline(XioPipelineFragment fragment) {
    // TODO(CK): interrogate fragment for channel options
    pipelineAssembler.addFragment(fragment);
    return this;
  }

  public XioServerBootstrap bindAddress(InetSocketAddress address) {
    serverBootstrap.localAddress(address);
    return this;
  }

  public XioServerBootstrap channelConfig(ChannelConfiguration channelConfig) {
    this.channelConfig = channelConfig;
    return this;
  }

  public XioServer build() {
    log.debug("Building");
    serverBootstrap.group(channelConfig.bossGroup(), channelConfig.workerGroup());
    serverBootstrap.channel(channelConfig.channel());
    final XioServerInstrumentation instrumentation = new XioServerInstrumentation();
    serverBootstrap.childHandler(pipelineAssembler.build(instrumentation));
    ChannelFuture future = serverBootstrap.bind();
    future.awaitUninterruptibly();
    if (future.isSuccess()) {
      instrumentation.addressBound = (InetSocketAddress)future.channel().localAddress();
    } else {
      log.error("Couldn't bind channel", future.cause());
      throw new RuntimeException(future.cause());
    }
    return new XioServer(future.channel(), instrumentation);
  }
}
