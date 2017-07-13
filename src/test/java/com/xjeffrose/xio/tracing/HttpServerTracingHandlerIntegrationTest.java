package com.xjeffrose.xio.tracing;

import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.fixtures.SampleHandler;
import com.xjeffrose.xio.pipeline.XioHttp1_1Pipeline;
import com.xjeffrose.xio.server.XioServer;


import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpTracing;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import java.io.IOException;
import java.util.logging.*;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.EXPECTATION_FAILED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


import brave.http.ITHttpServer;
import org.junit.After;
import org.junit.AssumptionViolatedException;
import org.junit.ComparisonFailure;
import org.junit.Test;

import static org.junit.Assert.*;

public class HttpServerTracingHandlerIntegrationTest extends ITHttpServer {

  static Logger disableJavaLogging() {
    Logger logger = Logger.getLogger("okhttp3.mockwebserver.MockWebServer");
    logger.setLevel(Level.WARNING);
    return logger;
  }

  Logger hush = disableJavaLogging();

  XioServer server = null;

  public static class BraveHandler extends SimpleChannelInboundHandler<HttpRequest> {
    private final HttpTracing httpTracing;
    public BraveHandler(HttpTracing httpTracing) {
      this.httpTracing = httpTracing;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
      String content = "Here is the default content that is returned";
      HttpResponseStatus status = OK;
      if (request.uri().startsWith("/foo")) {
      } else if (request.uri().startsWith("/child")) {

        Tracer tracer = httpTracing.tracing().tracer();

        Span parent = HttpTracingState.getSpan(ctx);
        Span span = tracer.newChild(parent.context()).name("child").start();
        //        System.out.println("channelRead0: " + span);
        span.finish();

      } else if (request.uri().startsWith("/exception")) {
        throw new IOException("exception");
      } else if (request.uri().startsWith("/async")) {
      } else if (request.uri().startsWith("/badrequest")) {
        status = BAD_REQUEST;
      } else {//not found
        status = NOT_FOUND;
      }

      writeResponse(ctx, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
    }

    private void writeResponse(ChannelHandlerContext ctx, HttpResponseStatus responseStatus, ByteBuf content) {

      // Build the response object.
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, responseStatus, content);

      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

      // Write the response.
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      // Write the response.
      writeResponse(ctx, EXPECTATION_FAILED, Unpooled.EMPTY_BUFFER);
    }
  }


  @Override
  protected void init() throws Exception {

    HttpServerTracingState state = new HttpServerTracingState(httpTracing, false);
    Function<Boolean, ChannelHandler> tracingHandler = b -> new HttpServerTracingHandler(state);
    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testApplication")
      .addToPipeline(new XioHttp1_1Pipeline(() -> new BraveHandler(httpTracing)))
      .configureServerState(s -> s.setTracingHandler(tracingHandler))
    ;

    server = bootstrap.build();
  }

  @Override
  protected String url(String path) {
    return "http://localhost:" + server.getInstrumentation().boundAddress().getPort() + path;
  }

  @After
  public void stop() throws Exception {
    if (server != null) {
      server.close();
    }
  }
}
