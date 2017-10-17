package com.xjeffrose.xio.pipeline;

import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.helpers.EchoClient;
import com.xjeffrose.xio.helpers.EchoServer;
import com.xjeffrose.xio.server.XioServer;
import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Test;

public class XioTcpProxyPipelineFunctionalTest extends Assert {

  @Test
  public void testProxyServer() {

    try (EchoClient client = new EchoClient(); EchoServer server = new EchoServer()) {
      server.bind(new InetSocketAddress("127.0.0.1", 0));
      XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testTcpProxyServer")
        .addToPipeline(new XioTcpProxyPipeline(server.addressBound()))
      ;
      try (XioServer proxy = bootstrap.build()) {
        client.connect(proxy.getInstrumentation().addressBound());
        server.accept();
        String payload = "test message";
        client.send(payload);
        String line = server.process();
        String response = client.recv();
        assertEquals(payload, line);
        assertEquals(payload, response);
      }
    }
  }

  @Test
  public void testEchoServerLargePayload() {

    try (EchoClient client = new EchoClient(); EchoServer server = new EchoServer()) {
      server.bind(new InetSocketAddress("127.0.0.1", 0));
      XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testTcpProxyServer")
        .addToPipeline(new XioTcpProxyPipeline(server.addressBound()))
      ;
      try (XioServer proxy = bootstrap.build()) {
        client.connect(proxy.getInstrumentation().addressBound());
        server.accept();
        int n = 800;
        String payload = "Netty rocks!";
        StringBuilder builder = new StringBuilder(n * payload.length());
        for (int i = 0; i < n; i += 1) {
          builder.append(payload);
        }
        payload = builder.toString();
        client.send(payload);
        String line = server.process();
        String response = client.recv();
        assertEquals(payload, line);
        assertEquals(payload, response);
      }
    }
  }

}
