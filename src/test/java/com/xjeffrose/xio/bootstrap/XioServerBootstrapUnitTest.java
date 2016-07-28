package com.xjeffrose.xio.bootstrap;

import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.pipeline.XioEchoPipeline;
import com.xjeffrose.xio.pipeline.XioHttp1_1Pipeline;
import com.xjeffrose.xio.pipeline.XioHttp2Pipeline;
import com.xjeffrose.xio.pipeline.XioHttpPipeline;
import com.xjeffrose.xio.server.XioRandomServerEndpoint;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import org.junit.Test;
import org.junit.Assert;

public class XioServerBootstrapUnitTest extends Assert {

  @Test
  public void buildHttp11Server() {
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.exampleServer");
    XioServerState serverState = XioServerState.fromConfig("xio.exampleApplication");

    XioServerBootstrap bootstrap = new XioServerBootstrap(serverConfig, serverState)
      .addToPipeline(new XioHttp1_1Pipeline())
      .channelConfig(ChannelConfiguration.serverConfig(1, 1))
      .endpoint(new XioRandomServerEndpoint())
    ;

    XioServer server = bootstrap.build();

    assertEquals("http/1.1", server.instrumentation().applicationProtocol());
  }

  @Test
  public void buildHttp2Server() {
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.exampleServer");
    XioServerState serverState = XioServerState.fromConfig("xio.exampleApplication");

    XioServerBootstrap bootstrap = new XioServerBootstrap(serverConfig, serverState)
      .addToPipeline(new XioHttp2Pipeline())
      .channelConfig(ChannelConfiguration.serverConfig(1, 1))
      .endpoint(new XioRandomServerEndpoint())
    ;

    XioServer server = bootstrap.build();

    assertEquals("http/2", server.instrumentation().applicationProtocol());
  }

  @Test
  public void buildTcpServer() {
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.exampleServer");
    XioServerState serverState = XioServerState.fromConfig("xio.exampleApplication");

    XioServerBootstrap bootstrap = new XioServerBootstrap(serverConfig, serverState)
      .addToPipeline(new XioEchoPipeline())
      .channelConfig(ChannelConfiguration.serverConfig(1, 1))
      .endpoint(new XioRandomServerEndpoint())
    ;

    XioServer server = bootstrap.build();

    assertEquals("echo", server.instrumentation().applicationProtocol());
  }

}
