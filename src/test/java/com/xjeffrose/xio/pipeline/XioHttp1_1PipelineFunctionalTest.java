package com.xjeffrose.xio.pipeline;

import com.squareup.okhttp.Response;
import com.xjeffrose.xio.fixtures.SampleHandler;
import com.xjeffrose.xio.helpers.ClientHelper;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.server.XioServer;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerState;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

public class XioHttp1_1PipelineFunctionalTest extends Assert {

  @Test
  public void testProxyServer() throws IOException {
    XioServerConfig serverConfig = XioServerConfig.fromConfig("xio.exampleServer");
    XioServerState serverState = XioServerState.fromConfig("xio.exampleApplication");

    XioServerBootstrap bootstrap = new XioServerBootstrap(serverConfig, serverState)
      .addToPipeline(new XioHttp1_1Pipeline(() -> new SampleHandler()))
    ;
    try (XioServer server = bootstrap.build()) {
      InetSocketAddress address = server.instrumentation().addressBound();
      Response response = ClientHelper.http(address);

      String expectedResponse = "WELCOME TO THE WILD WILD WEB SERVER\r\n" +
        "===================================\r\n" +
        "VERSION: HTTP/1.1\r\n" +
        "HOSTNAME: 127.0.0.1:" + address.getPort() + "\r\n" +
        "REQUEST_URI: /\r\n" +
        "\r\n" +
        "HEADER: Host = 127.0.0.1:" + address.getPort() + "\r\n" +
        "HEADER: Connection = Keep-Alive\r\n" +
        "HEADER: Accept-Encoding = gzip\r\n" +
        "HEADER: User-Agent = okhttp/2.4.0\r\n" +
        "\r\n" +
        "END OF CONTENT\r\n"
      ;

      assertTrue(response.isSuccessful());
      assertEquals(200, response.code());
      assertEquals(expectedResponse, response.body().string());
    }
  }

}
