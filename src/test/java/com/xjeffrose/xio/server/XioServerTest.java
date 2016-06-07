package com.xjeffrose.xio.server;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XioServerTest {

  @Test
  public void testConstructor() throws Exception {
    XioServerDef def = new XioServerDef();
    XioService xioService = new XioService();

    XioServer XioServer = new XioServer("testService", xioService);

  }
}
