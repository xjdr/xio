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
import io.netty.channel.ChannelHandler;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

public class XioServerBootstrapFunctionalTest {

  @Test
  public void testServe404() {
    XioRandomServerEndpoint endpoint = new XioRandomServerEndpoint();
    XioServerBootstrap bootstrap = new XioServerBootstrap()
      .addToPipeline(new XioHttpPipeline(new XioPipelineFragment() {
        public List<ChannelHandler> buildHandlers() {
          return Arrays.asList(
            new XioHttp404Handler()
          );
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
