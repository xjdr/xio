package com.xjeffrose.xio.client.loadbalancer;

import com.google.common.collect.ImmutableList;
import com.xjeffrose.xio.client.XioConnectionPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeTest {

  @Mock
  XioConnectionPool connectionPool;

  @Mock
  InetSocketAddress address;

  @Mock
  Future<Channel> futureChannel;

  @Captor
  ArgumentCaptor<FutureListener<Channel>> futureListenerChannelCaptor;

  ImmutableList<String> filters;
  int weight;
  String serviceName;
  Protocol proto;
  boolean ssl;
  Bootstrap bootstrap;

  @Before
  public void setUp() {
    filters = ImmutableList.of();
    weight = 1;
    serviceName = "testserv";
    proto = Protocol.TCP;
    ssl = false;
    bootstrap = new Bootstrap();
  }

  @Test
  public void testSend_successfulWrite() throws Exception {
    EmbeddedChannel channel = new EmbeddedChannel();
    bootstrap.group(channel.eventLoop());
    when(connectionPool.acquire()).thenReturn(futureChannel);
    Node node = createNode();
    Object message = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");

    Future<Void> sendFuture = node.send(message);

    assertEquals(false, sendFuture.isSuccess());
    verify(futureChannel).addListener(futureListenerChannelCaptor.capture());

    when(futureChannel.isSuccess()).thenReturn(true);
    when(futureChannel.getNow()).thenReturn(channel);

    futureListenerChannelCaptor.getValue().operationComplete(futureChannel);

    assertEquals(true, sendFuture.isSuccess());
    assertEquals(message, channel.outboundMessages().poll());
    verify(connectionPool).release(channel);
  }

  @Test
  public void testSend_failedWrite() throws Exception {
    final RuntimeException cause = new RuntimeException("testing error on write");
    EmbeddedChannel channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
      @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
        throws Exception {
        throw cause;
      }
    });
    bootstrap.group(channel.eventLoop());
    when(connectionPool.acquire()).thenReturn(futureChannel);
    Node node = createNode();
    Object message = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");

    Future<Void> sendFuture = node.send(message);

    assertEquals(false, sendFuture.isSuccess());
    verify(futureChannel).addListener(futureListenerChannelCaptor.capture());

    when(futureChannel.isSuccess()).thenReturn(true);
    when(futureChannel.getNow()).thenReturn(channel);

    futureListenerChannelCaptor.getValue().operationComplete(futureChannel);

    assertEquals(false, sendFuture.isSuccess());
    assertEquals(cause, sendFuture.cause());
    assertEquals(0, channel.outboundMessages().size());
    verify(connectionPool).release(channel);
  }

  @Test
  public void testSend_failedAcquire() throws Exception {
    EmbeddedChannel channel = new EmbeddedChannel();
    bootstrap.group(channel.eventLoop());
    when(connectionPool.acquire()).thenReturn(futureChannel);
    Node node = createNode();
    Object message = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");

    Future<Void> sendFuture = node.send(message);

    assertEquals(false, sendFuture.isSuccess());
    verify(futureChannel).addListener(futureListenerChannelCaptor.capture());

    when(futureChannel.isSuccess()).thenReturn(false);
    Throwable cause = new Throwable("ran out of resources");
    when(futureChannel.cause()).thenReturn(cause);

    futureListenerChannelCaptor.getValue().operationComplete(futureChannel);

    assertEquals(false, sendFuture.isSuccess());
    assertEquals(cause, sendFuture.cause());
    assertEquals(0, channel.outboundMessages().size());
    verify(connectionPool, never()).release(channel);
  }

  private Node createNode() {
    return new Node(address, filters, weight, serviceName, proto, ssl, bootstrap, connectionPool);
  }
}
