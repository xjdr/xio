package com.xjeffrose.xio.bootstrap;

import static org.junit.Assert.assertEquals;

import okhttp3.Response;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.helpers.ClientHelper;
import com.xjeffrose.xio.http.XioHttp404Handler;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import io.netty.channel.ChannelPipeline;
import org.junit.Test;

public class XioServerBootstrapFunctionalTest {

  @Test
  public void testServe404() {
    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testHttpServer")
      .addToPipeline(new SmartHttpPipeline(new XioPipelineFragment() {
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
      assertEquals(404, response.code());
      assertEquals(server.getInstrumentation() != null, true);
    }
  }
}
