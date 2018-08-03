package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.fixtures.SampleHandler;
import com.xjeffrose.xio.helpers.ClientHelper;
import com.xjeffrose.xio.server.XioServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.Test;

public class XioHttp1_1PipelineFunctionalTest extends Assert {

  @Test
  public void testProxyServer() throws IOException {
    XioServerBootstrap bootstrap =
        XioServerBootstrap.fromConfig("xio.testHttpServer")
            .addToPipeline(new SmartHttpPipeline(() -> new SampleHandler()));
    try (XioServer server = bootstrap.build()) {
      InetSocketAddress address = server.getInstrumentation().addressBound();
      Response response = ClientHelper.http(address);

      String expectedResponse =
          "WELCOME TO THE WILD WILD WEB SERVER\r\n"
              + "===================================\r\n"
              + "VERSION: HTTP/1.1\r\n"
              + "HOSTNAME: 127.0.0.1:"
              + address.getPort()
              + "\r\n"
              + "REQUEST_URI: /\r\n"
              + "\r\n"
              + "HEADER: Host = 127.0.0.1:"
              + address.getPort()
              + "\r\n"
              + "HEADER: Connection = Keep-Alive\r\n"
              + "HEADER: Accept-Encoding = gzip\r\n"
              + "HEADER: User-Agent = okhttp/3.11.0\r\n"
              + "\r\n"
              + "END OF CONTENT\r\n";

      assertTrue(response.isSuccessful());
      assertEquals(200, response.code());
      assertEquals(expectedResponse, response.body().string());
    }
  }
}
