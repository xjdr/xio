package com.xjeffrose.xio.tracing;

import static org.junit.Assert.*;

import java.util.logging.*;

/*
public class HttpServerTracingHandlerIntegrationTest extends ITHttpServer {

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
        HttpRequest request = (HttpRequest)msg;
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
    XioServerBootstrap bootstrap = XioServerBootstrap.fromConfig("xio.testHttpServer")
      .addToPipeline(new SmartHttpPipeline(() -> new BraveHandler(httpTracing)))
      .configureServerState(s -> *s.setTracingHandler(tracingHandler))
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
*/
