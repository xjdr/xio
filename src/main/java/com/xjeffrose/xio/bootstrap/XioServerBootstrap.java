package com.xjeffrose.xio.bootstrap;

import com.xjeffrose.xio.pipeline.XioPipelineAssembler;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerEndpoint;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XioServerBootstrap {
  private static final Logger log = LoggerFactory.getLogger(XioServerBootstrap.class);

  private final ServerBootstrap serverBootstrap;

  private final XioPipelineAssembler pipelineAssembler;

  private XioServerEndpoint endpoint;

  private ChannelConfiguration channelConfig;

  public XioServerBootstrap() {
    serverBootstrap = new ServerBootstrap();
    pipelineAssembler = new XioPipelineAssembler();
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
    serverBootstrap.childHandler(pipelineAssembler.build());
    ChannelFuture future = serverBootstrap.bind();
    endpoint.afterBind(future);
    future.awaitUninterruptibly();
    return new XioServer(future.channel());
  }
}
