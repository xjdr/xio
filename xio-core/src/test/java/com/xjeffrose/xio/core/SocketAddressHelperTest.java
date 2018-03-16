package com.xjeffrose.xio.core;

import lombok.val;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class SocketAddressHelperTest extends Assert {

  @Test
  public void testNotInetSocketAddress() {
    val socketAddress = new SocketAddress(){};
    val result = SocketAddressHelper.extractRemoteAddress(socketAddress);
    assertTrue(result == null);
  }


  @Test
  public void testValidRemoteAddress() {
    val inetSocketAddress = new InetSocketAddress(123);
    val result = SocketAddressHelper.extractRemoteAddress(inetSocketAddress);
    assertTrue(result.equals("0.0.0.0"));
  }
}
