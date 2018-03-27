package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.when;

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import com.xjeffrose.xio.tracing.HttpClientTracingHandler;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.val;
import okhttp3.Protocol;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.Arrays;

public class ClientTest extends Assert {

  private Client subject;
  MockWebServer server;
  @Mock private XioTracing tracing;
  @Mock private HttpClientTracingHandler tracingHandler;

  ChannelHandler appHandler =
      new ChannelHandler() {
        @Override
        public void handlerAdded(ChannelHandlerContext channelHandlerContext) throws Exception {}

        @Override
        public void handlerRemoved(ChannelHandlerContext channelHandlerContext) throws Exception {}

        @Override
        public void exceptionCaught(
            ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {}
      };

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    TlsConfig tlsConfig =
      TlsConfig.fromConfig("xio.h2BackendServer.settings.tls", ConfigFactory.load());
    server = OkHttpUnsafe.getSslMockWebServer(tlsConfig);
    server.setProtocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1));
    server.start();
  }

  @After
  public void tearDown() throws Exception {
    server.close();
  }

  @Test
  public void testDisabledTracing() {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig = new ClientConfig(ConfigFactory.load().getConfig("xio.basicClient"));
    val clientState = new ClientState(channelConfig, clientConfig);

    subject = new Client(clientState, () -> appHandler, null);

    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    subject.write(request);

    // Assert that we did not call handlerAdded when the tracing/traceHandler is null
    // This would crash if we tried adding a null handler, thus no explicit assertion
  }

  MockResponse buildResponse() {
    return new MockResponse().setBody("hello, world").setSocketPolicy(SocketPolicy.KEEP_OPEN);
  }

  @Test
  public void testEnabledTracing() throws Exception {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig = new ClientConfig(ConfigFactory.load().getConfig("xio.basicClient"));
    val clientState = new ClientState(channelConfig, clientConfig);
    when(tracing.newClientHandler(clientConfig.getTls().isUseSsl())).thenReturn(tracingHandler);

    val mockedUrl = server.url("/hello");

    subject = new Client(clientState, () -> appHandler, tracing);
    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .host(mockedUrl.host())
            .path("/hello")
            .build();

    server.enqueue(buildResponse());
    subject.write(request).sync();
    // Assert that we did DO call handlerAdded when the tracing/traceHandler is non-null
    try {
      Mockito.verify(tracingHandler, times(1)).handlerAdded(any(ChannelHandlerContext.class));
    } catch (Exception e) {
      fail();
    }
  }
}
