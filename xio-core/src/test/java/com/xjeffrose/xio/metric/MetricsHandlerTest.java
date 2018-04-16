package com.xjeffrose.xio.metric;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.xjeffrose.xio.http.DefaultFullRequest;
import com.xjeffrose.xio.http.DefaultFullResponse;
import com.xjeffrose.xio.http.DefaultHeaders;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MetricsHandlerTest extends Assert {

  private EmbeddedChannel channel;

  @Mock
  MetricRegistry metricRegistry;

  @Mock
  Meter requestsMeter;

  @Mock
  Meter statusClassInformationalMeter;

  @Mock
  Meter statusClassSuccessMeter;

  @Mock
  Meter statusClassRedirectionMeter;

  @Mock
  Meter statusClassClientErrorMeter;

  @Mock
  Meter statusClassServerErrorMeter;

  @Mock
  Meter statusClassUnknownMeter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    metricRegistry = mock(MetricRegistry.class);

    Mockito.when(metricRegistry.meter("requests")).thenReturn(requestsMeter);
    Mockito.when(metricRegistry.meter("statusClassInformational")).thenReturn(statusClassInformationalMeter);
    Mockito.when(metricRegistry.meter("statusClassSuccess")).thenReturn(statusClassSuccessMeter);
    Mockito.when(metricRegistry.meter("statusClassRedirection")).thenReturn(statusClassRedirectionMeter);
    Mockito.when(metricRegistry.meter("statusClassClientError")).thenReturn(statusClassClientErrorMeter);
    Mockito.when(metricRegistry.meter("statusClassServerError")).thenReturn(statusClassServerErrorMeter);
    Mockito.when(metricRegistry.meter("statusClassUnknown")).thenReturn(statusClassUnknownMeter);

    MetricsHandler metricsHandler = new MetricsHandler(metricRegistry);

    channel = new EmbeddedChannel();
    channel
      .pipeline()
      .addLast(metricsHandler);
  }

  @After
  public void tearDown() {

  }

  @Test
  public void testRequestsMeter() {
    DefaultFullRequest request = DefaultFullRequest.builder()
      .body(Unpooled.EMPTY_BUFFER)
      .headers(new DefaultHeaders())
      .method(HttpMethod.GET)
      .path("/foo")
      .build();

    channel.writeInbound(request);
    channel.runPendingTasks();

    verify(requestsMeter).mark();
  }

  @Test
  public void testStatusClassInformationalMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.CONTINUE));
    channel.runPendingTasks();

    verify(statusClassInformationalMeter).mark();
  }

  @Test
  public void testStatusClassSuccessMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.OK));
    channel.runPendingTasks();

    verify(statusClassSuccessMeter).mark();
  }

  @Test
  public void testStatusClassRedirectionMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.MULTIPLE_CHOICES));
    channel.runPendingTasks();

    verify(statusClassRedirectionMeter).mark();
  }

  @Test
  public void testStatusClassClientErrorMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.BAD_REQUEST));
    channel.runPendingTasks();

    verify(statusClassClientErrorMeter).mark();
  }

  @Test
  public void testStatusClassServerErrorMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR));
    channel.runPendingTasks();

    verify(statusClassServerErrorMeter).mark();
  }

  @Test
  public void testStatusClassUnknownMeter() {
    channel.writeOutbound(buildResponse(new HttpResponseStatus(600, "unknown")));
    channel.runPendingTasks();

    verify(statusClassUnknownMeter).mark();
  }

  private DefaultFullResponse buildResponse(HttpResponseStatus httpResponseStatus) {
    return DefaultFullResponse.builder()
      .body(Unpooled.EMPTY_BUFFER)
      .headers(new DefaultHeaders())
      .status(httpResponseStatus)
      .build();
  }
}
