package com.xjeffrose.xio.tracing;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static org.junit.Assert.*;

import brave.Tracer;
import brave.http.HttpTracing;
import com.xjeffrose.xio.bootstrap.XioServerBootstrap;
import com.xjeffrose.xio.fixtures.JulBridge;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.server.XioServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import java.io.IOException;
import java.util.logging.*;
import org.junit.After;
import org.junit.BeforeClass;

// TODO(CK): These brave integration tests are flaky and stall out sometimes
// Turn them back on when they are fixed
public class HttpServerTracingHandlerIntegrationTest { // extends ITHttpServer {

  @BeforeClass
  public static void setupJul() {
    JulBridge.initialize();
  }

  static Logger disableJavaLogging() {
    Logger logger = Logger.getLogger("okhttp3.mockwebserver.MockWebServer");
    logger.setLevel(Level.WARNING);
    return logger;
  }

  Logger hush = disableJavaLogging();

  XioServer server = null;

  public static class BraveHandler extends SimpleChannelInboundHandler<HttpObject> {
    private final HttpTracing httpTracing;

    public BraveHandler(HttpTracing httpTracing) {
      this.httpTracing = httpTracing;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
      if (msg instanceof HttpRequest) {
        HttpRequest request = (HttpRequest) msg;
        String content = "Here is the default content that is returned";
        HttpResponseStatus status = OK;
        if (request.uri().startsWith("/foo")) {
        } else if (request.uri().startsWith("/child")) {

          Tracer tracer = httpTracing.tracing().tracer();

          // Span parent = HttpTracingState.getSpan(ctx);
          // Span span = tracer.newChild(parent.context()).name("child").start();
          //        System.out.println("channelRead0: " + span);
          // span.finish();

        } else if (request.uri().startsWith("/exception")) {
          throw new IOException("exception");
        } else if (request.uri().startsWith("/async")) {
        } else if (request.uri().startsWith("/badrequest")) {
          status = BAD_REQUEST;
        } else { // not found
          status = NOT_FOUND;
        }

        writeResponse(ctx, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
      }
    }

    private void writeResponse(
        ChannelHandlerContext ctx, HttpResponseStatus responseStatus, ByteBuf content) {

      // Build the response object.
      DefaultFullHttpResponse response =
          new DefaultFullHttpResponse(HTTP_1_1, responseStatus, content);

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

  // @Override
  protected void init() throws Exception {

    HttpTracing httpTracing = null; // TODO(CK): remove this when the tests are fixed
    XioServerBootstrap bootstrap =
        XioServerBootstrap.fromConfig("xio.testHttpServer")
            .addToPipeline(new SmartHttpPipeline(() -> new BraveHandler(httpTracing)));

    server = bootstrap.build();
  }

  // @Override
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
