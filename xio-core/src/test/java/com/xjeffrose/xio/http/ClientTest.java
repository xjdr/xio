package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.when;

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.tracing.HttpClientTracingHandler;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.val;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

public class ClientTest extends Assert {

  private Client subject;
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
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void tearDown() {
    Mockito.reset(tracing);
    Mockito.reset(tracingHandler);
  }

  @Test
  public void testDisabledTracing() {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig =
        new ClientConfig(ConfigFactory.load().getConfig("xio.invalidZipkinParameters"));
    val clientState = new ClientState(channelConfig, clientConfig);

    subject =
        new Client(
            clientState,
            () -> {
              return appHandler;
            },
            null);

    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    val writeFuture = subject.write(request);

    // Assert that we did not call handlerAdded when the tracing/traceHandler is null
    // This would crash if we tried adding a null handler, thus no explicit assertion
  }

  @Test
  public void testEnabledTracing() {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig =
        new ClientConfig(ConfigFactory.load().getConfig("xio.validZipkinParameters"));
    val clientState = new ClientState(channelConfig, clientConfig);
    when(tracing.newClientHandler(clientConfig.getTls().isUseSsl())).thenReturn(tracingHandler);

    subject =
        new Client(
            clientState,
            () -> {
              return appHandler;
            },
            tracing);
    Request request =
        DefaultFullRequest.builder()
            .body(Unpooled.EMPTY_BUFFER)
            .headers(new DefaultHeaders())
            .method(GET)
            .path("/")
            .build();
    val writeFuture = subject.write(request);

    // Assert that we did DO call handlerAdded when the tracing/traceHandler is non-null
    try {
      ArgumentCaptor<ChannelHandlerContext> captor =
          ArgumentCaptor.forClass(ChannelHandlerContext.class);
      Mockito.verify(tracingHandler, times(1)).handlerAdded(captor.capture());
    } catch (Exception e) {
      fail();
    }
  }
}
