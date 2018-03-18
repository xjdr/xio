package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.fixtures.JulBridge;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

@Slf4j
public class ReverseProxyFunctionalTest extends Assert {

  @BeforeClass
  public static void setupJul() {
    JulBridge.initialize();
  }

  OkHttpClient client;
  Config config;
  EventLoopGroup group;
  ApplicationConfig appConfig;
  Application reverseProxy;
  MockWebServer server;

  static Application setupReverseProxy(ApplicationConfig appConfig, ProxyRouteConfig proxyConfig) {
    ClientFactory factory =
        new ClientFactory() {
          @Override
          public Client createClient(ChannelHandlerContext ctx, ClientConfig config) {
            ClientState clientState = new ClientState(channelConfig(ctx), config, () -> null);
            return new Client(clientState, () -> new ProxyBackendHandler(ctx));
          }
        };

    return new ApplicationBootstrap(appConfig)
        .addServer(
            "main",
            (bs) ->
                bs.addToPipeline(
                    new SmartHttpPipeline() {
                      @Override
                      public ChannelHandler getApplicationRouter() {
                        return new PipelineRouter(
                            ImmutableMap.of(),
                            new ProxyHandler(factory, proxyConfig, new SocketAddressHelper()));
                      }
                    }))
        .build();
  }

  @Rule public TestName testName = new TestName();

  @Before
  public void setupCommon() {
    config = ConfigFactory.load();
    log.debug("Test: " + testName.getMethodName());
  }

  void setupBack(boolean h2) throws Exception {
    String back = h2 ? "h2" : "h1";

    TlsConfig tlsConfig =
        TlsConfig.fromConfig("xio." + back + "BackendServer.settings.tls", config);
    List<Protocol> protocols;
    if (h2) {
      protocols = Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1);
    } else {
      protocols = Arrays.asList(Protocol.HTTP_1_1);
    }

    server = OkHttpUnsafe.getSslMockWebServer(tlsConfig);
    server.setProtocols(protocols);
    server.start();
  }

  void setupFrontBack(boolean h2Front, boolean h2Back) throws Exception {
    setupBack(h2Back);
    int port = server.getPort();

    String front = h2Front ? "h2" : "h1";
    appConfig = ApplicationConfig.fromConfig("xio." + front + "ReverseProxy", config);
    // TODO(CK): this creates global state across tests we should do something smarter
    System.setProperty("xio.baseClient.remotePort", Integer.toString(server.getPort()));
    System.setProperty("xio.testProxyRoute.proxyPath", "/hello/");
    ConfigFactory.invalidateCaches();
    Config root = ConfigFactory.load();
    ProxyRouteConfig proxyConfig = new ProxyRouteConfig(root.getConfig("xio.testProxyRoute"));

    reverseProxy = setupReverseProxy(appConfig, proxyConfig);
  }

  void setupClient(boolean h2) throws Exception {
    if (h2) {
      client =
          OkHttpUnsafe.getUnsafeClient()
              .newBuilder()
              .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
              .build();
    } else {
      client =
          OkHttpUnsafe.getUnsafeClient()
              .newBuilder()
              .protocols(Arrays.asList(Protocol.HTTP_1_1))
              .build();
    }
  }

  @After
  public void tearDown() throws Exception {
    client.connectionPool().evictAll();
    if (reverseProxy != null) {
      reverseProxy.close();
    }
    server.close();
  }

  int port() {
    return reverseProxy.instrumentation("main").boundAddress().getPort();
  }

  String url(int port, boolean sanity) {
    StringBuilder path =
        new StringBuilder("https://").append("127.0.0.1").append(":").append(port).append("/");
    if (sanity) {
      path.append("hello/");
    }

    return path.toString();
  }

  MockResponse buildResponse() {
    return new MockResponse().setBody("hello, world").setSocketPolicy(SocketPolicy.KEEP_OPEN);
  }

  void get(int port, boolean sanity) throws Exception {
    String url = url(port, sanity);
    Request request = new Request.Builder().url(url).build();

    server.enqueue(buildResponse());
    Response response = client.newCall(request).execute();

    RecordedRequest servedRequest = server.takeRequest();
    assertEquals("/hello/", servedRequest.getRequestUrl().encodedPath());
  }

  void post(int port, boolean sanity) throws Exception {
    String url = url(port, sanity);
    MediaType mediaType = MediaType.parse("text/plain");
    RequestBody body = RequestBody.create(mediaType, "this is the post body");
    Request request = new Request.Builder().url(url).post(body).build();

    server.enqueue(buildResponse());
    Response response = client.newCall(request).execute();

    RecordedRequest servedRequest = server.takeRequest();
    assertEquals("/hello/", servedRequest.getRequestUrl().encodedPath());
    assertEquals("this is the post body", servedRequest.getBody().readUtf8());
  }

  @Test
  public void sanityCheckHttp1Get() throws Exception {
    setupClient(false);
    setupBack(false);

    get(server.getPort(), true);
  }

  @Test
  public void sanityCheckHttp1Post() throws Exception {
    setupClient(false);
    setupBack(false);

    post(server.getPort(), true);
  }

  @Test
  public void sanityCheckHttp2Get() throws Exception {
    setupClient(true);
    setupBack(true);

    get(server.getPort(), true);
  }

  @Test
  public void sanityCheckHttp2Post() throws Exception {
    setupClient(true);
    setupBack(true);

    post(server.getPort(), true);
  }

  @Test
  public void testHttp2toHttp1ServerGet() throws Exception {
    setupClient(true);
    setupFrontBack(true, false);

    get(port(), false);
  }

  @Test
  public void testHttp2toHttp1ServerPost() throws Exception {
    setupClient(true);
    setupFrontBack(true, false);

    post(port(), false);
  }

  @Test
  public void testHttp1toHttp2ServerGet() throws Exception {
    setupClient(false);
    setupFrontBack(false, true);

    get(port(), false);
  }

  @Test
  public void testHttp1toHttp2ServerPost() throws Exception {
    setupClient(false);
    setupFrontBack(false, true);

    post(port(), false);
  }
}
