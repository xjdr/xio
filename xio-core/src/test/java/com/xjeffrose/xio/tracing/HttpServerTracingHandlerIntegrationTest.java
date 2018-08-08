package com.xjeffrose.xio.tracing;

import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.StrictCurrentTraceContext;
import brave.sampler.Sampler;
import com.typesafe.config.Config;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.helpers.ClientHelper;
import com.xjeffrose.xio.http.DefaultFullResponse;
import com.xjeffrose.xio.http.DefaultHeaders;
import com.xjeffrose.xio.http.FullRequest;
import com.xjeffrose.xio.http.Request;
import com.xjeffrose.xio.http.SegmentedRequestData;
import com.xjeffrose.xio.http.TraceInfo;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.val;
import okhttp3.Protocol;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HttpServerTracingHandlerIntegrationTest extends Assert {

  private Application application = null;
  private CountDownLatch latch;

  @Before
  public void before() throws Exception {

    application =
        new ApplicationBootstrap("xio.testZipkinApplication", XioTracingDecorator::new)
            .addServer(
                "exampleServer", (bs) -> bs.addToPipeline(new SmartHttpPipeline(TestHandler::new)))
            .build();
  }

  @After
  public void stop() throws Exception {
    application.close();
  }

  @Test
  public void testSpanDispatchedH1() throws Exception {
    latch = new CountDownLatch(1);
    InetSocketAddress address = application.instrumentation("exampleServer").boundAddress();
    ClientHelper.https(address, Protocol.HTTP_1_1);
    latch.await(1, TimeUnit.SECONDS);
    assertEquals(0, latch.getCount());
  }

  @Test
  public void testSpanDispatchedH2() throws Exception {
    latch = new CountDownLatch(1);
    InetSocketAddress address = application.instrumentation("exampleServer").boundAddress();
    ClientHelper.https(address, Protocol.HTTP_2, Protocol.HTTP_1_1);
    latch.await(1, TimeUnit.SECONDS);
    assertEquals(0, latch.getCount());
  }

  private class XioTracingDecorator extends XioTracing {

    private CurrentTraceContext currentTraceContext;

    XioTracingDecorator(Config config) {
      super(config);
    }

    @Override
    protected Tracing buildTracing(String name, String zipkinUrl, float samplingRate) {
      if (currentTraceContext == null) {
        currentTraceContext = new StrictCurrentTraceContext();
      }

      return Tracing.newBuilder()
          .spanReporter(ignored -> latch.countDown())
          .currentTraceContext(currentTraceContext)
          .sampler(Sampler.ALWAYS_SAMPLE)
          .build();
    }
  }

  private class TestHandler extends SimpleChannelInboundHandler<Request> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request msg) throws Exception {

      if (msg instanceof SegmentedRequestData && msg.endOfMessage()) {
        sendResponse(ctx, msg.httpTraceInfo(), msg.streamId());
        return;
      } else if (msg instanceof FullRequest) {
        sendResponse(ctx, msg.httpTraceInfo(), msg.streamId());
      }

      ctx.write(msg);
    }

    private void sendResponse(ChannelHandlerContext ctx, TraceInfo traceInfo, int streamId) {
      val resp =
          DefaultFullResponse.builder()
              .httpTraceInfo(traceInfo)
              .headers(new DefaultHeaders())
              .streamId(streamId)
              .status(HttpResponseStatus.OK)
              .body(Unpooled.EMPTY_BUFFER)
              .build();
      ctx.writeAndFlush(resp);
    }
  }
}
