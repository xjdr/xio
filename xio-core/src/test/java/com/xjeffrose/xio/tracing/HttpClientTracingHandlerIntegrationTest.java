//package com.xjeffrose.xio.tracing;
//
//import static io.netty.handler.codec.http.HttpMethod.*;
//import static io.netty.handler.codec.http.HttpResponseStatus.*;
//import static io.netty.handler.codec.http.HttpVersion.*;
//import static java.nio.charset.StandardCharsets.UTF_8;
//import static org.junit.Assert.*;
//
//import brave.http.HttpTracing;
//import brave.propagation.TraceContext;
//import com.xjeffrose.xio.client.ClientConfig;
//import com.xjeffrose.xio.client.XioClient;
//import com.xjeffrose.xio.client.XioClientBootstrap;
//import com.xjeffrose.xio.client.XioRequest;
//import com.xjeffrose.xio.client.loadbalancer.Protocol;
//import com.xjeffrose.xio.fixtures.JulBridge;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.Unpooled;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.EventLoopGroup;
//import io.netty.channel.SimpleChannelInboundHandler;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.handler.codec.http.DefaultFullHttpRequest;
//import io.netty.handler.codec.http.HttpHeaderNames;
//import io.netty.handler.codec.http.HttpObject;
//import io.netty.handler.codec.http.HttpRequest;
//import io.netty.handler.codec.http.HttpResponse;
//import io.netty.handler.codec.http.LastHttpContent;
//import io.netty.util.concurrent.Future;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.SocketAddress;
//import java.util.concurrent.CompletableFuture;
//import java.util.logging.*;
//import org.junit.AssumptionViolatedException;
//import org.junit.BeforeClass;
//import org.junit.ComparisonFailure;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.rules.TestWatcher;
//import org.junit.runner.Description;
//
//// TODO(CK): These brave integration tests are flaky and stall out sometimes
//// Turn them back on when they are fixed
//public class HttpClientTracingHandlerIntegrationTest { // extends ITHttpClient<XioClient> {
//
//  @BeforeClass
//  public static void setupJul() {
//    JulBridge.initialize();
//  }
//
//  @Rule
//  public TestWatcher testWatcher =
//      new TestWatcher() {
//        @Override
//        protected void starting(final Description description) {
//          String methodName = description.getMethodName();
//          String className = description.getClassName();
//          className = className.substring(className.lastIndexOf('.') + 1);
//          // System.out.println("Starting JUnit-test: " + className + " " + methodName);
//        }
//      };
//
//  static Logger disableJavaLogging() {
//    Logger logger = Logger.getLogger("okhttp3.mockwebserver.MockWebServer");
//    logger.setLevel(Level.WARNING);
//    return logger;
//  }
//
//  Logger hush = disableJavaLogging();
//  CompletableFuture<HttpResponse> local = new CompletableFuture<HttpResponse>();
//
//  public class ApplicationHandler extends SimpleChannelInboundHandler<HttpObject> {
//
//    HttpResponse response;
//
//    @Override
//    public void channelRead0(ChannelHandlerContext ctx, HttpObject object) throws Exception {
//      if (object instanceof HttpResponse) {
//        response = (HttpResponse) object;
//        // System.out.println("Response: " + response);
//      }
//      if (object instanceof LastHttpContent) {
//        local.complete(response);
//      }
//    }
//
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//      // System.out.println("exceptionCaught: " + cause);
//      local.completeExceptionally(cause);
//    }
//  }
//
//  EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
//  HttpClientTracingState state;
//
//  protected XioClient newClient(int port) {
//    // System.out.println("newClient port: " + port);
//    HttpTracing httpTracing = null; // TODO(CK): remove this when the tests are fixed
//    state = new HttpClientTracingState(httpTracing, false);
//
//    return new XioClientBootstrap(ClientConfig.fromConfig("xio.h1TestClient"), eventLoopGroup)
//        .address(new InetSocketAddress("127.0.0.1", port))
//        .ssl(false)
//        .proto(Protocol.HTTP)
//        .handler(new ApplicationHandler())
//        .tracingHandler(() -> new HttpClientTracingHandler(state))
//        .usePool(false)
//        .build();
//  }
//
//  protected void closeClient(XioClient client) throws IOException {
//    // System.out.println("closeClient client: " + client);
//    client.close();
//  }
//
//  private XioRequest<HttpRequest> buildRequest(XioClient client, HttpRequest payload) {
//    if (!payload.headers().contains(HttpHeaderNames.HOST)) {
//      SocketAddress address = client.getBootstrap().config().remoteAddress();
//      if (address instanceof InetSocketAddress) {
//        InetSocketAddress socketAddress = (InetSocketAddress) address;
//        String value = socketAddress.getHostString() + ":" + socketAddress.getPort();
//        payload.headers().set(HttpHeaderNames.HOST, value);
//      }
//    }
//    TraceContext parent = state.getTracing().currentTraceContext().get();
//    XioRequest<HttpRequest> request = new XioRequest<>(payload, parent);
//    return request;
//  }
//
//  protected void get(XioClient client, String pathIncludingQuery) throws Exception {
//    // System.out.println("get client: " + client + " path: " + pathIncludingQuery);
//    HttpRequest payload = new DefaultFullHttpRequest(HTTP_1_1, GET, pathIncludingQuery);
//    XioRequest<HttpRequest> request = buildRequest(client, payload);
//    Future<Void> future = client.write(request);
//    future.awaitUninterruptibly();
//    try {
//      local.get();
//    } catch (Exception e) {
//      // System.out.println("caught e: " + e);
//    }
//  }
//
//  protected void post(XioClient client, String pathIncludingQuery, String body) throws Exception {
//    // System.out.println("post client: " + client + " path: " + pathIncludingQuery + " body: " +
//    // body);
//    ByteBuf content = Unpooled.copiedBuffer(body, UTF_8);
//    DefaultFullHttpRequest payload =
//        new DefaultFullHttpRequest(HTTP_1_1, POST, pathIncludingQuery, content);
//    payload.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
//    payload.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
//    XioRequest<HttpRequest> request = buildRequest(client, payload);
//    client.write(request).sync();
//    local.get();
//  }
//
//  protected void getAsync(XioClient client, String pathIncludingQuery) throws Exception {
//    // System.out.println("getAsync client: " + client + " path: " + pathIncludingQuery);
//    HttpRequest payload = new DefaultFullHttpRequest(HTTP_1_1, GET, pathIncludingQuery);
//    XioRequest<HttpRequest> request = buildRequest(client, payload);
//    client.write(request);
//  }
//
//  // @Override
//  @Test(expected = ComparisonFailure.class)
//  public void redirect() throws Exception {
//    throw new AssumptionViolatedException("client does not support redirect");
//  }
//
//  // @Override
//  @Test(expected = ComparisonFailure.class)
//  public void addsStatusCodeWhenNotOk() throws Exception {
//    throw new AssumptionViolatedException("test is flaky");
//  }
//
//  // @Override
//  @Test(expected = ComparisonFailure.class)
//  public void httpPathTagExcludesQueryParams() throws Exception {
//    throw new AssumptionViolatedException("test is flaky");
//  }
//}
