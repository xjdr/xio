package com.xjeffrose.xio.bootstrap;

import com.squareup.okhttp.Response;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.handler.XioHttp404Handler;
import com.xjeffrose.xio.helpers.ClientHelper;
import com.xjeffrose.xio.pipeline.XioHttpPipeline;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.server.XioRandomServerEndpoint;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelPipeline;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class XioServerBootstrapFunctionalTest {

  @Test
  public void testServe404() {
    XioRandomServerEndpoint endpoint = new XioRandomServerEndpoint();
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.exampleServer");

    XioServerBootstrap bootstrap = new XioServerBootstrap(serverConfig)
      .addToPipeline(new XioHttpPipeline(new XioPipelineFragment() {
        public String applicationProtocol() {
          return "";
        }
        public void buildHandlers(XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
          pipeline.addLast(new XioHttp404Handler());
        }
      }))
      .channelConfig(ChannelConfiguration.serverConfig(1, 1))
      .endpoint(endpoint)
    ;
    try (XioServer server = bootstrap.build()) {
      Response response = ClientHelper.request("http://" + endpoint.hostAndPort() + "/");
      assertEquals(response.code(), 404);
      assertEquals(server.running(), true);
    }
  }
}
