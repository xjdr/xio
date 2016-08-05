package com.xjeffrose.xio.server;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XioServiceTest {

  @Test
  public void testConstructor() throws Exception {
    XioService xioService = new XioService();

    assertEquals(xioService, xioService.andThen(xioService));
  }
}
