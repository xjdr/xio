package com.xjeffrose.xio.http;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpMethod;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClientTest extends Assert {

  private Client subject;
  private EmbeddedChannel channel;
  @Mock private ClientConnectionManager connectionManager;

  private Request requestFactory(String path) {
    DefaultFullRequest request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(HttpMethod.GET)
            .path(path)
            .build();
    return request;
  }

  @Before
  public void setUp() throws Exception {
    channel = new EmbeddedChannel();
    MockitoAnnotations.initMocks(this);
    when(connectionManager.currentChannel()).thenReturn(channel);
    subject = new Client(null, connectionManager);
  }

  @Test
  public void testAlreadyConnectedWrites() throws Exception {
    when(connectionManager.connectionState()).thenReturn(ClientConnectionState.CONNECTED);

    Request request1 = requestFactory("req1");
    Request request2 = requestFactory("req2");
    Optional<ChannelFuture> result1Future = subject.write(request1);
    Optional<ChannelFuture> result2Future = subject.write(request2);

    Request proxiedRequest1 = channel.readOutbound();
    Request proxiedRequest2 = channel.readOutbound();

    assertEquals(proxiedRequest1.path(), request1.path());
    assertEquals(proxiedRequest2.path(), request2.path());

    assertTrue(result1Future.get().isDone() && result1Future.get().isSuccess());
    assertTrue(result2Future.get().isDone() && result2Future.get().isSuccess());
  }

  @Test
  public void testInitialConnectedWrites() throws Exception {
    ChannelPromise connectPromise = channel.newPromise();
    when(connectionManager.connectionState()).thenReturn(ClientConnectionState.NOT_CONNECTED);
    when(connectionManager.connect()).thenReturn(connectPromise);
    Request request1 = requestFactory("req1");
    Optional<ChannelFuture> result1Future = subject.write(request1);

    verify(connectionManager).connect();

    Request proxiedRequest1BeforeConnection = channel.readOutbound();
    assertNull(proxiedRequest1BeforeConnection);

    connectPromise.setSuccess();

    Request proxiedRequest1AfterConnectionComplete = channel.readOutbound();
    assertEquals(proxiedRequest1AfterConnectionComplete.path(), request1.path());

    assertTrue(result1Future.get().isDone() && result1Future.get().isSuccess());
  }

  @Test
  public void testConnectionInProcessWrites() throws Exception {
    ChannelPromise connectPromise = channel.newPromise();
    when(connectionManager.connectionState()).thenReturn(ClientConnectionState.NOT_CONNECTED);
    when(connectionManager.connect()).thenReturn(connectPromise);
    Request request1 = requestFactory("req1");
    Request request2 = requestFactory("req2");
    Request request3 = requestFactory("req3");
    Optional<ChannelFuture> result1Future = subject.write(request1);
    when(connectionManager.connectionState()).thenReturn(ClientConnectionState.CONNECTING);
    Optional<ChannelFuture> result2Future = subject.write(request2);
    Optional<ChannelFuture> result3Future = subject.write(request3);

    // shouldn't get anything written on the channel at all
    Request proxiedRequest1BeforeConnection = channel.readOutbound();
    assertNull(proxiedRequest1BeforeConnection);

    connectPromise.setSuccess();

    Request proxiedRequest1AfterConnectionComplete = channel.readOutbound();
    Request proxiedRequest2AfterConnectionComplete = channel.readOutbound();
    Request proxiedRequest3AfterConnectionComplete = channel.readOutbound();
    assertEquals(proxiedRequest1AfterConnectionComplete.path(), request1.path());
    assertEquals(proxiedRequest2AfterConnectionComplete.path(), request2.path());
    assertEquals(proxiedRequest3AfterConnectionComplete.path(), request3.path());

    assertTrue(result1Future.get().isDone() && result1Future.get().isSuccess());
    assertTrue(result2Future.get().isDone() && result2Future.get().isSuccess());
    assertTrue(result3Future.get().isDone() && result3Future.get().isSuccess());
  }

  @Test
  public void testConnectinFailed() throws Exception {
    when(connectionManager.connectionState()).thenReturn(ClientConnectionState.CLOSED_CONNECTION);

    Request request1 = requestFactory("req1");
    Optional<ChannelFuture> result1Future = subject.write(request1);

    assertFalse(result1Future.isPresent());
  }
}
