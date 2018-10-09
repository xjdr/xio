package com.xjeffrose.xio.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xjeffrose.xio.client.ClientConfig;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class RoundRobinProxyHandlerTest extends Assert {

  RoundRobinProxyHandler subject;

  @Mock ProxyRouteConfig config;

  @Mock ClientConfig clientConfig1;
  @Mock ClientConfig clientConfig2;
  @Mock ClientConfig clientConfig3;

  List<ClientConfig> clientConfigs;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    clientConfigs = Arrays.asList(clientConfig1, clientConfig2, clientConfig3);
  }

  @Test
  public void testFullRequests() {
    when(config.clientConfigs()).thenReturn(clientConfigs);
    subject = new RoundRobinProxyHandler(null, config, null);

    Request request1 = mock(Request.class);
    Request request2 = mock(Request.class);
    Request request3 = mock(Request.class);
    Request request4 = mock(Request.class);

    // since we are doing all full requests we actually don't care what the request path is
    when(request1.isFullMessage()).thenReturn(true);
    when(request1.path()).thenReturn("dontcare");
    when(request1.streamId()).thenReturn(Message.H1_STREAM_ID_NONE);
    when(request2.isFullMessage()).thenReturn(true);
    when(request2.path()).thenReturn("dontcare");
    when(request2.streamId()).thenReturn(Message.H1_STREAM_ID_NONE);
    when(request3.isFullMessage()).thenReturn(true);
    when(request3.path()).thenReturn("dontcare");
    when(request3.streamId()).thenReturn(Message.H1_STREAM_ID_NONE);
    when(request4.isFullMessage()).thenReturn(true);
    when(request4.path()).thenReturn("dontcare");
    when(request4.streamId()).thenReturn(Message.H1_STREAM_ID_NONE);

    Map<String, Optional<ClientConfig>> cacheMap = new HashMap<>();
    ClientConfig result1 = subject.getClientConfig(cacheMap, request1).orElse(null);
    assertEquals(clientConfig1, result1);

    ClientConfig result2 = subject.getClientConfig(cacheMap, request2).orElse(null);
    assertEquals(clientConfig2, result2);

    ClientConfig result3 = subject.getClientConfig(cacheMap, request3).orElse(null);
    assertEquals(clientConfig3, result3);

    // loops around
    ClientConfig result4 = subject.getClientConfig(cacheMap, request3).orElse(null);
    assertEquals(clientConfig1, result4);

    assertTrue(cacheMap.isEmpty());
  }

  @Test
  public void testChunkedRequestsHttp1() {
    when(config.clientConfigs()).thenReturn(clientConfigs);
    subject = new RoundRobinProxyHandler(null, config, null);

    Request request1 = mock(Request.class);
    Request request2 = mock(Request.class);
    Request request3 = mock(Request.class);

    // first part of a chunked message
    when(request1.isFullMessage()).thenReturn(false);
    when(request1.startOfMessage()).thenReturn(true);
    when(request1.endOfMessage()).thenReturn(false);
    when(request1.path()).thenReturn("/foo");
    when(request1.streamId()).thenReturn(Message.H1_STREAM_ID_NONE);

    // middle part of a chunked message
    when(request2.isFullMessage()).thenReturn(false);
    when(request2.startOfMessage()).thenReturn(false);
    when(request2.endOfMessage()).thenReturn(false);
    when(request2.path()).thenReturn("/foo");
    when(request2.streamId()).thenReturn(Message.H1_STREAM_ID_NONE);

    // last part of a chunked message
    when(request3.isFullMessage()).thenReturn(false);
    when(request3.startOfMessage()).thenReturn(false);
    when(request3.endOfMessage()).thenReturn(true);
    when(request3.path()).thenReturn("/foo");
    when(request3.streamId()).thenReturn(Message.H1_STREAM_ID_NONE);

    Map<String, Optional<ClientConfig>> cacheMap = new HashMap<>();
    ClientConfig result1 = subject.getClientConfig(cacheMap, request1).orElse(null);
    assertEquals(clientConfig1, result1);

    ClientConfig result2 = subject.getClientConfig(cacheMap, request2).orElse(null);
    assertEquals(clientConfig1, result2);

    ClientConfig result3 = subject.getClientConfig(cacheMap, request3).orElse(null);
    assertEquals(clientConfig1, result3);

    assertTrue(cacheMap.isEmpty());
  }

  @Test
  public void testChunkedRequestsHttp2() {
    when(config.clientConfigs()).thenReturn(clientConfigs);
    subject = new RoundRobinProxyHandler(null, config, null);

    int streamId1 = 1;
    int streamId2 = 3;

    Map<String, Optional<ClientConfig>> cacheMap = new HashMap<>();

    Request request1a = mock(Request.class);
    Request request2a = mock(Request.class);
    Request request3a = mock(Request.class);
    Request request1b = mock(Request.class);
    Request request2b = mock(Request.class);
    Request request3b = mock(Request.class);

    // lets interleave requests between a and b

    // first part of a chunked message #1
    when(request1a.isFullMessage()).thenReturn(false);
    when(request1a.startOfMessage()).thenReturn(true);
    when(request1a.endOfMessage()).thenReturn(false);
    when(request1a.path()).thenReturn("/foo");
    when(request1a.streamId()).thenReturn(streamId1);

    // first part of a chunked message #2
    when(request1b.isFullMessage()).thenReturn(false);
    when(request1b.startOfMessage()).thenReturn(true);
    when(request1b.endOfMessage()).thenReturn(false);
    when(request1b.path()).thenReturn("/foo");
    when(request1b.streamId()).thenReturn(streamId2);

    // middle part of a chunked message #1
    when(request2a.isFullMessage()).thenReturn(false);
    when(request2a.startOfMessage()).thenReturn(false);
    when(request2a.endOfMessage()).thenReturn(false);
    when(request2a.path()).thenReturn("/foo");
    when(request2a.streamId()).thenReturn(streamId1);

    // middle part of a chunked message #2
    when(request2b.isFullMessage()).thenReturn(false);
    when(request2b.startOfMessage()).thenReturn(false);
    when(request2b.endOfMessage()).thenReturn(false);
    when(request2b.path()).thenReturn("/foo");
    when(request2b.streamId()).thenReturn(streamId2);

    // last part of a chunked message #1
    when(request3a.isFullMessage()).thenReturn(false);
    when(request3a.startOfMessage()).thenReturn(false);
    when(request3a.endOfMessage()).thenReturn(true);
    when(request3a.path()).thenReturn("/foo");
    when(request3a.streamId()).thenReturn(streamId1);

    // last part of a chunked message #2
    when(request3b.isFullMessage()).thenReturn(false);
    when(request3b.startOfMessage()).thenReturn(false);
    when(request3b.endOfMessage()).thenReturn(true);
    when(request3b.path()).thenReturn("/foo");
    when(request3b.streamId()).thenReturn(streamId2);

    ClientConfig result1a = subject.getClientConfig(cacheMap, request1a).orElse(null);
    assertEquals(clientConfig1, result1a);
    ClientConfig result1b = subject.getClientConfig(cacheMap, request1b).orElse(null);
    assertEquals(clientConfig2, result1b);

    ClientConfig result2a = subject.getClientConfig(cacheMap, request2a).orElse(null);
    assertEquals(clientConfig1, result2a);
    ClientConfig result2b = subject.getClientConfig(cacheMap, request2b).orElse(null);
    assertEquals(clientConfig2, result2b);

    ClientConfig result3a = subject.getClientConfig(cacheMap, request3a).orElse(null);
    assertEquals(clientConfig1, result3a);
    ClientConfig result3b = subject.getClientConfig(cacheMap, request3b).orElse(null);
    assertEquals(clientConfig2, result3b);

    assertTrue(cacheMap.isEmpty());
  }
}
