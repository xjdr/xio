package com.xjeffrose.xio.metric;

import static org.junit.Assert.assertEquals;

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
import org.junit.Test;

public class HttpServerMetricHandlerIntegrationTest {
  private Application application = null;
  private MetricRegistry metricRegistry = null;

  @Before
  public void before() {
    application =
        new ApplicationBootstrap("xio.testZipkinApplication")
            .addServer(
                "exampleServer", (bs) -> bs.addToPipeline(new SmartHttpPipeline(TestHandler::new)))
            .build();

    metricRegistry = application.getState().getMetricRegistry();
  }

  @After
  public void stop() {
    application.close();
  }

  @Test
  public void testRequestMetricH1() throws Exception {
    InetSocketAddress address = application.instrumentation("exampleServer").boundAddress();
    ClientHelper.https(address, Protocol.HTTP_1_1);
    ClientHelper.https(address, Protocol.HTTP_1_1);

    Meter meter = metricRegistry.meter("requests");
    assertEquals(2, meter.getCount());
  }

  @Test
  public void testRequestMetricH2() throws Exception {
    InetSocketAddress address = application.instrumentation("exampleServer").boundAddress();
    ClientHelper.https(address, Protocol.HTTP_2, Protocol.HTTP_1_1);
    ClientHelper.https(address, Protocol.HTTP_2, Protocol.HTTP_1_1);

    Meter meter = metricRegistry.meter("requests");
    assertEquals(2, meter.getCount());
  }

  @Test
  public void teststatusClassMetricH1() throws Exception {
    InetSocketAddress address = application.instrumentation("exampleServer").boundAddress();
    ClientHelper.https(address, Protocol.HTTP_1_1);
    ClientHelper.https(address, Protocol.HTTP_1_1);

    Meter meter = metricRegistry.meter("statusClassSuccess");
    assertEquals(2, meter.getCount());
  }

  @Test
  public void teststatusClassMetricH2() throws Exception {
    InetSocketAddress address = application.instrumentation("exampleServer").boundAddress();
    ClientHelper.https(address, Protocol.HTTP_2, Protocol.HTTP_1_1);
    ClientHelper.https(address, Protocol.HTTP_2, Protocol.HTTP_1_1);

    Meter meter = metricRegistry.meter("statusClassSuccess");
    assertEquals(2, meter.getCount());
  }

  private class TestHandler extends SimpleChannelInboundHandler<Request> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request msg) {

      if (msg instanceof SegmentedRequestData && msg.endOfMessage()) {
        sendResponse(ctx);
        return;
      } else if (msg instanceof FullRequest) {
        sendResponse(ctx);
      }

      ctx.write(msg);
    }

    private void sendResponse(ChannelHandlerContext ctx) {
      val resp =
          DefaultFullResponse.builder()
              .headers(new DefaultHeaders())
              .status(HttpResponseStatus.OK)
              .body(Unpooled.EMPTY_BUFFER)
              .build();
      ctx.writeAndFlush(resp);
    }
  }
}
