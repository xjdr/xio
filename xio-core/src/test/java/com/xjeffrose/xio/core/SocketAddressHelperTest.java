package com.xjeffrose.xio.core;

import static org.powermock.api.mockito.PowerMockito.when;

import io.netty.channel.Channel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import java.net.InetAddress;
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

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testNonSocketOrSocketServerChannel() throws Exception {
    val result = SocketAddressHelper.extractRemoteAddress(channel);
    assertTrue(result == null);
  }

  @Test
  public void testWithoutInetSocketAddressSocketServerChannel() throws Exception {
    when(serverSocketChannel.remoteAddress()).thenReturn(null);

    val result = SocketAddressHelper.extractRemoteAddress(serverSocketChannel);
    assertTrue(result == null);
  }

  @Test
  public void testInetSocketAddressSocketChannel() throws Exception {
    when(socketChannel.remoteAddress()).thenReturn(null);

    val result = SocketAddressHelper.extractRemoteAddress(socketChannel);
    assertTrue(result == null);
  }

  @Test
  public void testValidAddressSocketServerChannel() throws Exception {
    val inetSocketAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 123);
    when(serverSocketChannel.remoteAddress()).thenReturn(inetSocketAddress);

    val result = SocketAddressHelper.extractRemoteAddress(serverSocketChannel);
    assertTrue(result.equals("127.0.0.1"));
  }

  @Test
  public void testValidAddressSocketChannel() throws Exception {
    val inetSocketAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 123);
    when(socketChannel.remoteAddress()).thenReturn(inetSocketAddress);

    val result = SocketAddressHelper.extractRemoteAddress(socketChannel);
    assertTrue(result.equals("127.0.0.1"));
  }
}
