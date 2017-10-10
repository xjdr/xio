package com.xjeffrose.xio.bootstrap;

import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.server.XioServer;
import org.junit.Test;
import org.junit.Assert;

public class XioServerBootstrapUnitTest extends Assert {

  @Test
  public void buildHttp11Server() {
    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testHttpsServer")
      .addToPipeline(new SmartHttpPipeline())
    ;

    XioServer server = bootstrap.build();

    assertEquals("ssl-http/1.1", server.getInstrumentation().applicationProtocol());
  }

}
