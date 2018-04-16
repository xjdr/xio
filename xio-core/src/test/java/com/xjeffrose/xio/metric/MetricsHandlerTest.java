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

public class MetricsHandlerTest extends Assert {

  private EmbeddedChannel channel;
  private MetricRegistry metricRegistry;

  @Before
  public void setUp() {
    metricRegistry = new MetricRegistry();

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

    Meter meter = metricRegistry.meter("requests");
    assertEquals(1, meter.getCount());
  }

  @Test
  public void testStatusClassInformationalMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.CONTINUE));
    channel.runPendingTasks();

    Meter meter = metricRegistry.meter("statusClassInformational");
    assertEquals(1, meter.getCount());
  }

  @Test
  public void testStatusClassSuccessMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.OK));
    channel.runPendingTasks();

    Meter meter = metricRegistry.meter("statusClassSuccess");
    assertEquals(1, meter.getCount());
  }

  @Test
  public void testStatusClassRedirectionMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.MULTIPLE_CHOICES));
    channel.runPendingTasks();

    Meter meter = metricRegistry.meter("statusClassRedirection");
    assertEquals(1, meter.getCount());
  }

  @Test
  public void testStatusClassClientErrorMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.BAD_REQUEST));
    channel.runPendingTasks();

    Meter meter = metricRegistry.meter("statusClassClientError");
    assertEquals(1, meter.getCount());
  }

  @Test
  public void testStatusClassServerErrorMeter() {
    channel.writeOutbound(buildResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR));
    channel.runPendingTasks();

    Meter meter = metricRegistry.meter("statusClassServerError");
    assertEquals(1, meter.getCount());
  }

  @Test
  public void testStatusClassUnknownMeter() {
    channel.writeOutbound(buildResponse(new HttpResponseStatus(600, "unknown")));
    channel.runPendingTasks();

    Meter meter = metricRegistry.meter("statusClassUnknown");
    assertEquals(1, meter.getCount());
  }

  private DefaultFullResponse buildResponse(HttpResponseStatus httpResponseStatus) {
    return DefaultFullResponse.builder()
      .body(Unpooled.EMPTY_BUFFER)
      .headers(new DefaultHeaders())
      .status(httpResponseStatus)
      .build();
  }
}
