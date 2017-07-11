package com.xjeffrose.xio.tracing;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpTracing;
import brave.internal.StrictCurrentTraceContext;
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import java.util.concurrent.ConcurrentLinkedDeque;

import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import java.io.IOException;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.EXPECTATION_FAILED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.HttpMethod.GET;


import org.junit.Test;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;


public class HttpServerTracingHandlerTest extends Assert {

  public static class ApplicationHandler extends SimpleChannelInboundHandler<HttpRequest> {
    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
      ByteBuf content = Unpooled.copiedBuffer("Here is the default content that is returned", CharsetUtil.UTF_8);
      HttpResponseStatus status = OK;

      Tracer tracer = HttpServerTracingState.tracer(ctx);
      Span span = tracer.nextSpan().name("child").start();
      Tracer.SpanInScope inScope = Tracing.currentTracer().withSpanInScope(span);
      span.finish();
      inScope.close();

      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, content);

      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
  }


  ConcurrentLinkedDeque<zipkin.Span> spans = new ConcurrentLinkedDeque<>();

  CurrentTraceContext currentTraceContext = new StrictCurrentTraceContext();
  HttpTracing httpTracing;
  HttpServerTracingState httpTracingState;

  Tracing.Builder tracingBuilder(Sampler sampler) {
    return Tracing.newBuilder()
      .reporter(s -> {
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
    httpTracingState = new HttpServerTracingState(httpTracing, false);
  }

  @Test
  public void testThreadBoundaries() throws Exception {

    Thread thread = new Thread(new Runnable() {
        public void run() {
          EmbeddedChannel channel = new EmbeddedChannel(new HttpServerTracingHandler(httpTracingState),
                                                        new ApplicationHandler());

          DefaultHttpRequest request = new DefaultHttpRequest(HTTP_1_1, GET, "/foo");
          channel.writeInbound(request);
          channel.runPendingTasks();

          synchronized(httpTracing) {
            httpTracing.notify();
          }
        }
      });
    thread.start();
    synchronized(httpTracing) {
      httpTracing.wait();
    }
    Assert.assertEquals(2, spans.size());
  }

}
