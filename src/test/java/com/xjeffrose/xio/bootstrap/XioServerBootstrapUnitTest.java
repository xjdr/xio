package com.xjeffrose.xio.bootstrap;

import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.pipeline.XioEchoPipeline;
import com.xjeffrose.xio.pipeline.XioHttp1_1Pipeline;
import com.xjeffrose.xio.pipeline.XioHttp2Pipeline;
import com.xjeffrose.xio.server.XioServer;
import org.junit.Test;
import org.junit.Assert;

public class XioServerBootstrapUnitTest extends Assert {

  @Test
  public void buildHttp11Server() {
    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testApplication")
      .addToPipeline(new XioHttp1_1Pipeline())
    ;

    XioServer server = bootstrap.build();

    assertEquals("http/1.1", server.getInstrumentation().applicationProtocol());
  }

  @Test
  public void buildHttp2Server() {
    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testApplication")
      .addToPipeline(new XioHttp2Pipeline())
    ;

    XioServer server = bootstrap.build();

    assertEquals("http/2", server.getInstrumentation().applicationProtocol());
  }

  @Test
  public void buildTcpServer() {
    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testApplication")
      .addToPipeline(new XioEchoPipeline())
    ;

    XioServer server = bootstrap.build();

    assertEquals("echo", server.getInstrumentation().applicationProtocol());
  }

}
