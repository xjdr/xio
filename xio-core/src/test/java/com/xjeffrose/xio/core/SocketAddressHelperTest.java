package com.xjeffrose.xio.core;

import static org.powermock.api.mockito.PowerMockito.when;

import io.netty.channel.Channel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import java.net.InetSocketAddress;
import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SocketAddressHelperTest extends Assert {
  @Mock private ServerSocketChannel serverSocketChannel;
  @Mock private SocketChannel socketChannel;
  @Mock private Channel channel;
  private SocketAddressHelper subject;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    subject = new SocketAddressHelper();
  }

  @Test
  public void testNonSocketOrSocketServerChannel() throws Exception {
    val result = subject.extractRemoteAddressAndPort(channel);
    assertTrue(result == null);
  }

  @Test
  public void testWithoutInetSocketAddressSocketServerChannel() throws Exception {
    when(serverSocketChannel.remoteAddress()).thenReturn(null);

    val result = subject.extractRemoteAddressAndPort(serverSocketChannel);
    assertTrue(result == null);
  }

  @Test
  public void testInetSocketAddressSocketChannel() throws Exception {
    when(socketChannel.remoteAddress()).thenReturn(null);

    val result = subject.extractRemoteAddressAndPort(socketChannel);
    assertTrue(result == null);
  }

  @Test
  public void testValidAddressSocketServerChannel() throws Exception {
    val testPort = 123;
    val testAddress = "192.168.0.1";
    val inetSocketAddress = new InetSocketAddress(testAddress, testPort);
    val expectedResult = testAddress + ":" + String.valueOf(testPort);
    when(serverSocketChannel.remoteAddress()).thenReturn(inetSocketAddress);

    val result = subject.extractRemoteAddressAndPort(serverSocketChannel);
    assertTrue(result.equals(expectedResult));
  }

  @Test
  public void testValidAddressSocketChannel() throws Exception {
    val testPort = 2;
    val testAddress = "192.168.0.1";
    val inetSocketAddress = new InetSocketAddress(testAddress, testPort);
    val expectedResult = testAddress + ":" + String.valueOf(testPort);
    when(socketChannel.remoteAddress()).thenReturn(inetSocketAddress);
    val result = subject.extractRemoteAddressAndPort(socketChannel);
    assertTrue(result.equals(expectedResult));
  }
}
