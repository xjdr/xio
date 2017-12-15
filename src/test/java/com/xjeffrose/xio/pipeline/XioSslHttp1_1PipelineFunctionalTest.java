package com.xjeffrose.xio.pipeline;

import okhttp3.Response;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.fixtures.SamplePipelineRequestHandler;
import com.xjeffrose.xio.fixtures.SimpleTestServer;
import com.xjeffrose.xio.helpers.ClientHelper;
import com.xjeffrose.xio.helpers.HttpProxyServer;
import com.xjeffrose.xio.helpers.HttpsProxyServer;
import com.xjeffrose.xio.helpers.ProxyPipelineRequestHandler;
import com.xjeffrose.xio.http.PipelineRouter;
import com.xjeffrose.xio.server.XioServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Assert;
import org.junit.Test;
import com.google.common.collect.ImmutableMap;
import io.netty.channel.ChannelHandler;

public class XioSslHttp1_1PipelineFunctionalTest extends Assert {

  @Test
  public void testServer() throws IOException {
    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testHttpsServer")
      .addToPipeline(new SmartHttpPipeline() {
        @Override
        public ChannelHandler getApplicationRouter() {
          return new PipelineRouter(ImmutableMap.of(), new SamplePipelineRequestHandler());
        }
      })
    ;
    try (XioServer server = bootstrap.build()) {
      InetSocketAddress address = server.getInstrumentation().addressBound();
      Response response = ClientHelper.https(address);

      String expectedResponse = "WELCOME TO THE WILD WILD WEB SERVER\r\n" +
        "===================================\r\n" +
        "VERSION: HTTP/1.1\r\n" +
        "HOSTNAME: 127.0.0.1:" + address.getPort() + "\r\n" +
        "REQUEST_URI: /\r\n" +
        "\r\n" +
        "HEADER: Host = 127.0.0.1:" + address.getPort() + "\r\n" +
        "HEADER: Connection = Keep-Alive\r\n" +
        "HEADER: Accept-Encoding = gzip\r\n" +
        "HEADER: User-Agent = okhttp/3.9.1\r\n" +
        "\r\n" +
        "END OF CONTENT\r\n"
      ;

      assertTrue(response.isSuccessful());
      assertEquals(200, response.code());
      assertEquals(expectedResponse, response.body().string());
    }
  }

  @Test
  public void testProxyToHttpServer() throws IOException {
    try (SimpleTestServer testServer = new SimpleTestServer(0)) {
      testServer.run();

      XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testHttpsServer")
        .addToPipeline(new SmartHttpPipeline() {
          @Override
          public ChannelHandler getApplicationRouter() {
            return new PipelineRouter(ImmutableMap.of(), new ProxyPipelineRequestHandler(testServer.boundAddress(), false));
          }
        })
        ;

      try (XioServer server = bootstrap.build()) {
        InetSocketAddress address = server.getInstrumentation().addressBound();
        Response response = ClientHelper.https(address);

        assertTrue(response.isSuccessful());
        assertEquals(200, response.code());
        assertEquals("Jetty(9.3.1.v20150714)", response.header("Server"));
        assertEquals("CONGRATS!\n", response.body().string());
      }
    }
  }

  //  @Test
  // TODO(CK): This is actually an integration test and a flaky one at that
  public void testProxyToHttpsServer() throws IOException, URISyntaxException {
    URI uri = new URI("https://www.paypal.com:443/home");

    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testHttpsServer")
      .addToPipeline(new SmartHttpPipeline(new HttpsProxyServer(uri)))
      ;

    try (XioServer server = bootstrap.build()) {
      InetSocketAddress address = server.getInstrumentation().addressBound();
      Response response = ClientHelper.https(address);

      assertTrue(response.isSuccessful());
      assertEquals(200, response.code());
      assertEquals("Apache", response.header("Server"));
      assertEquals("If you are reading this, maybe you should be working at PayPal instead! Check out https://www.paypal.com/us/webapps/mpp/paypal-jobs", response.header("X-Recruiting"));
      assertFalse(response.body().string().isEmpty());
    }
  }

}
