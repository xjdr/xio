package com.xjeffrose.xio.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;

import static org.junit.Assert.*;

public class BBtoHttpResponseTest {

  @Test
  public void testGetResponseOK() throws Exception {
    ByteBuf bb = Unpooled.wrappedBuffer("HTTP/1.1 200 OK\r\nServer: xio\r\n\r\n\r\n".getBytes());
    DefaultFullHttpResponse response = BBtoHttpResponse.getResponse(bb);

    assertEquals(HttpResponseStatus.OK, response.getStatus());
    assertEquals("xio", response.headers().get("Server"));

  }

  @Test
  public void testGetResponseServerError() throws Exception {
    ByteBuf bb = Unpooled.wrappedBuffer("HTTP/1.1 500 Internal Server Error\r\nServer: xio\r\n\r\n\r\n".getBytes());
    DefaultFullHttpResponse response = BBtoHttpResponse.getResponse(bb);

    assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.getStatus());
    assertEquals("xio", response.headers().get("Server"));
  }
}