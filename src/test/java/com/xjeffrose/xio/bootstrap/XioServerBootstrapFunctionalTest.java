package com.xjeffrose.xio.bootstrap;

import com.squareup.okhttp.Response;
import com.xjeffrose.xio.handler.XioHttp404Handler;
import com.xjeffrose.xio.helpers.ClientHelper;
import com.xjeffrose.xio.pipeline.XioHttpPipeline;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelPipeline;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class XioServerBootstrapFunctionalTest {

  @Test
  public void testServe404() {
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.exampleServer");
    XioServerState serverState = XioServerState.fromConfig("xio.exampleApplication");

    XioServerBootstrap bootstrap = new XioServerBootstrap(serverConfig, serverState)
      .addToPipeline(new XioHttpPipeline(new XioPipelineFragment() {
        public String applicationProtocol() {
          return "";
        }
        public void buildHandlers(XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
          pipeline.addLast(new XioHttp404Handler());
        }
      }))
    ;
    try (XioServer server = bootstrap.build()) {
      Response response = ClientHelper.http(server.instrumentation().addressBound());
      assertEquals(response.code(), 404);
      assertEquals(server.instrumentation() != null, true);
    }
  }
}
