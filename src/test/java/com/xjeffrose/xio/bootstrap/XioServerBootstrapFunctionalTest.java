package com.xjeffrose.xio.bootstrap;

import com.squareup.okhttp.Response;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.handler.XioHttp404Handler;
import com.xjeffrose.xio.helpers.ClientHelper;
import com.xjeffrose.xio.pipeline.XioHttp1_1Pipeline;
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
    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testApplication")
      .addToPipeline(new XioHttp1_1Pipeline(new XioPipelineFragment() {
        public String applicationProtocol() {
          return "";
        }
        public void buildHandlers(ApplicationState appState, XioServerConfig config, XioServerState state, ChannelPipeline pipeline) {
          pipeline.addLast(new XioHttp404Handler());
        }
      }))
    ;
    try (XioServer server = bootstrap.build()) {
      Response response = ClientHelper.http(server.getInstrumentation().addressBound());
      assertEquals(response.code(), 404);
      assertEquals(server.getInstrumentation() != null, true);
    }
  }
}
