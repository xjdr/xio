package com.xjeffrose.xio.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class XioServiceTest {

  @Test
  public void testConstructor() throws Exception {
    XioService xioService = new XioService();

    assertEquals(xioService, xioService.andThen(xioService));
  }
}
