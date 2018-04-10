package com.xjeffrose.xio.tracing;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpTracing;
import brave.internal.StrictCurrentTraceContext;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import com.xjeffrose.xio.http.DefaultFullResponse;
import com.xjeffrose.xio.http.DefaultHeaders;
import com.xjeffrose.xio.http.Http1ServerCodec;
import com.xjeffrose.xio.http.Request;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import java.util.concurrent.ConcurrentLinkedDeque;
import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HttpServerTracingHandlerTest extends Assert {

  public class ApplicationHandler extends SimpleChannelInboundHandler<Request> {
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
      ByteBuf content =
          Unpooled.copiedBuffer("Here is the default content that is returned", CharsetUtil.UTF_8);
      HttpResponseStatus status = OK;

      Tracer tracer = httpTracing.tracing().tracer();

      request
          .httpTraceInfo()
          .getSpan()
          .ifPresent(
              parent -> {
                Span span = tracer.newChild(parent.context()).name("child").start();
                span.finish();
              });

      val response =
          DefaultFullResponse.builder()
              .status(status)
              .headers(new DefaultHeaders())
              .httpTraceInfo(request.httpTraceInfo())
              .status(status)
              .body(content)
              .build();

      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
  }

  ConcurrentLinkedDeque<zipkin.Span> spans = new ConcurrentLinkedDeque<>();

  CurrentTraceContext currentTraceContext = new StrictCurrentTraceContext();
  HttpTracing httpTracing;
  HttpServerTracingDispatch httpTracingState;

  Tracing.Builder tracingBuilder(Sampler sampler) {
    return Tracing.newBuilder()
        .reporter(
            s -> {
              // make sure the context was cleared prior to finish.. no leaks!
              TraceContext current = httpTracing.tracing().currentTraceContext().get();
              if (current != null) {
                Assert.assertNotEquals(current.spanId(), s.id);
              }
              spans.add(s);
            })
        .currentTraceContext(currentTraceContext)
        .sampler(sampler);
  }

  @Before
  public void setup() throws Exception {
    httpTracing = HttpTracing.create(tracingBuilder(Sampler.ALWAYS_SAMPLE).build());
    httpTracingState = new HttpServerTracingDispatch(httpTracing, false);
  }

  @Test
  public void testThreadBoundaries() throws Exception {

    Thread thread =
        new Thread(
            () -> {
              EmbeddedChannel channel =
                  new EmbeddedChannel(
                      new Http1ServerCodec(),
                      new HttpServerTracingHandler(httpTracingState),
                      new ApplicationHandler());
              DefaultHttpRequest request = new DefaultHttpRequest(HTTP_1_1, GET, "/foo");
              channel.writeInbound(request);
              channel.runPendingTasks();

              synchronized (httpTracing) {
                httpTracing.notify();
              }
            });
    thread.start();
    synchronized (httpTracing) {
      httpTracing.wait();
    }
    Assert.assertEquals(2, spans.size());
  }
}
