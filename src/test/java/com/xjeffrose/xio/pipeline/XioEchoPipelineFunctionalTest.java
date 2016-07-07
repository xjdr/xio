package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.helpers.EchoClient;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.server.XioRandomServerEndpoint;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import org.junit.Assert;
import org.junit.Test;

public class XioEchoPipelineFunctionalTest extends Assert {

  @Test
  public void testEchoServer() {
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.exampleServer");
    XioServerState serverState = XioServerState.fromConfig("xio.exampleApplication");

    XioServerBootstrap bootstrap = new XioServerBootstrap(serverConfig, serverState)
      .addToPipeline(new XioEchoPipeline())
      .channelConfig(ChannelConfiguration.serverConfig(1, 1))
      .endpoint(new XioRandomServerEndpoint())
    ;

    try (XioServer server = bootstrap.build(); EchoClient client = new EchoClient()) {
      client.connect(server.instrumentation().addressBound());
      String payload = "test message";
      client.send(payload);
      String response = client.recv();
      assertEquals(payload, response);
    }
  }

}
