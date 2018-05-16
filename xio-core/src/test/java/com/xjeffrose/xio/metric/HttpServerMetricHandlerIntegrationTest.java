package com.xjeffrose.xio.metric;

import static org.mockito.Mockito.*;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.helpers.ClientHelper;
import com.xjeffrose.xio.http.*;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.net.InetSocketAddress;
import lombok.val;
import okhttp3.Protocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("flaky test - fix me")
public class HttpServerMetricHandlerIntegrationTest {
  private Application application = null;
  private Meter requestsMeter = null;
  private Meter successMeter = null;

  @Before
  public void before() {
    application =
        new ApplicationBootstrap("xio.testZipkinApplication")
            .addServer(
                "exampleServer", (bs) -> bs.addToPipeline(new SmartHttpPipeline(TestHandler::new)))
            .build();

    MetricRegistry metricRegistry = mock(MetricRegistry.class);

    requestsMeter = mock(Meter.class);
    when(metricRegistry.meter("requests")).thenReturn(requestsMeter);
    successMeter = mock(Meter.class);
    when(metricRegistry.meter("statusClassSuccess")).thenReturn(successMeter);

    application.getState().setMetricRegistry(metricRegistry);
  }

  @After
  public void tearDown() {
    application.close();
  }

  @Test
  public void testMetricsH1() throws Exception {
    InetSocketAddress address = application.instrumentation("exampleServer").boundAddress();
    ClientHelper.https(address, Protocol.HTTP_1_1);
    ClientHelper.https(address, Protocol.HTTP_1_1);

    verify(requestsMeter, times(2)).mark();
    verify(successMeter, times(2)).mark();
  }

  @Test
  public void testMetricsH2() throws Exception {
    InetSocketAddress address = application.instrumentation("exampleServer").boundAddress();
    ClientHelper.https(address, Protocol.HTTP_2, Protocol.HTTP_1_1);
    ClientHelper.https(address, Protocol.HTTP_2, Protocol.HTTP_1_1);

    verify(requestsMeter, times(2)).mark();
    verify(successMeter, times(2)).mark();
  }

  private class TestHandler extends SimpleChannelInboundHandler<Request> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request msg) {

      if (msg instanceof SegmentedRequestData && msg.endOfMessage()) {
        sendResponse(ctx, msg);
        return;
      } else if (msg instanceof FullRequest) {
        sendResponse(ctx, msg);
      }

      ctx.write(msg);
    }

    private void sendResponse(ChannelHandlerContext ctx, Request msg) {
      val resp =
          DefaultFullResponse.builder()
              .headers(new DefaultHeaders())
              .status(HttpResponseStatus.OK)
              .body(Unpooled.EMPTY_BUFFER)
              .streamId(msg.streamId())
              .build();
      ctx.writeAndFlush(resp);
    }
  }
}
