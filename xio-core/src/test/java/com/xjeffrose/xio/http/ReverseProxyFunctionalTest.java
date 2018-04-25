package com.xjeffrose.xio.http;

import static com.xjeffrose.xio.helpers.TlsHelper.getKeyManagers;
import static okhttp3.Protocol.HTTP_1_1;
import static okhttp3.Protocol.HTTP_2;

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
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import com.xjeffrose.xio.tracing.XioTracing;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import net.jodah.concurrentunit.Waiter;
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
import org.junit.*;
import org.junit.rules.TestName;

@Slf4j
public class ReverseProxyFunctionalTest extends Assert {

  @BeforeClass
  public static void setupJul() {
    JulBridge.initialize();
  }

  OkHttpClient client;
  Config config;
  ApplicationConfig appConfig;
  Application reverseProxy;
  MockWebServer server;

  static Application setupReverseProxy(
      ApplicationConfig appConfig, ProxyRouteConfig proxyConfig, XioTracing tracing) {
    ClientFactory factory =
        new ClientFactory(tracing) {
          @Override
          public Client createClient(ChannelHandlerContext ctx, ClientConfig config) {
            ClientState clientState = new ClientState(channelConfig(ctx), config);
            return new Client(clientState, () -> new ProxyBackendHandler(ctx), getTracing());
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
      protocols = Arrays.asList(HTTP_2, HTTP_1_1);
    } else {
      protocols = Collections.singletonList(HTTP_1_1);
    }

    server = OkHttpUnsafe.getSslMockWebServer(getKeyManagers(tlsConfig));
    server.setProtocols(protocols);
    server.start();
  }

  void setupFrontBack(boolean h2Front, boolean h2Back) throws Exception {
    setupBack(h2Back);

    String front = h2Front ? "h2" : "h1";
    appConfig = ApplicationConfig.fromConfig("xio." + front + "ReverseProxy", config);
    // TODO(CK): this creates global state across tests we should do something smarter
    System.setProperty("xio.baseClient.remotePort", Integer.toString(server.getPort()));
    System.setProperty("xio.testProxyRoute.proxyPath", "/hello/");
    ConfigFactory.invalidateCaches();
    Config root = ConfigFactory.load();
    ProxyRouteConfig proxyConfig = new ProxyRouteConfig(root.getConfig("xio.testProxyRoute"));

    reverseProxy =
        setupReverseProxy(
            appConfig, proxyConfig, new XioTracing(root.getConfig("xio.testProxyRoute")));
  }

  void setupClient(boolean h2) throws Exception {
    if (h2) {
      client =
          OkHttpUnsafe.getUnsafeClient()
              .newBuilder()
              .protocols(Arrays.asList(HTTP_2, HTTP_1_1))
              .build();
    } else {
      client =
          OkHttpUnsafe.getUnsafeClient()
              .newBuilder()
              .protocols(Collections.singletonList(HTTP_1_1))
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

  void get(int port, boolean sanity, Protocol expectedProtocol) throws Exception {
    String url = url(port, sanity);
    Request request = new Request.Builder().url(url).build();

    server.enqueue(buildResponse());
    Response response = client.newCall(request).execute();
    assertEquals(expectedProtocol, response.protocol());

    RecordedRequest servedRequest = server.takeRequest();
    assertEquals("/hello/", servedRequest.getRequestUrl().encodedPath());
  }

  void post(int port, boolean sanity, Protocol expectedProtocol) throws Exception {
    String url = url(port, sanity);
    MediaType mediaType = MediaType.parse("text/plain");
    RequestBody body = RequestBody.create(mediaType, "this is the post body");
    Request request = new Request.Builder().url(url).post(body).build();

    server.enqueue(buildResponse());
    Response response = client.newCall(request).execute();
    assertEquals("unexpected client response protocol", expectedProtocol, response.protocol());

    RecordedRequest servedRequest = server.takeRequest();
    assertEquals("/hello/", servedRequest.getRequestUrl().encodedPath());
    assertEquals("this is the post body", servedRequest.getBody().readUtf8());
  }

  @Test
  public void sanityCheckHttp1Get() throws Exception {
    setupClient(false);
    setupBack(false);

    get(server.getPort(), true, HTTP_1_1);
  }

  @Test
  public void sanityCheckHttp1Post() throws Exception {
    setupClient(false);
    setupBack(false);

    post(server.getPort(), true, HTTP_1_1);
  }

  @Test
  public void sanityCheckHttp2Get() throws Exception {
    setupClient(true);
    setupBack(true);

    get(server.getPort(), true, HTTP_2);
  }

  @Test
  public void sanityCheckHttp2Post() throws Exception {
    setupClient(true);
    setupBack(true);

    post(server.getPort(), true, HTTP_2);
  }

  @Test
  public void testHttp2toHttp1ServerGet() throws Exception {
    setupClient(true);
    setupFrontBack(true, false);

    get(port(), false, HTTP_2);
  }

  @Test
  public void testHttp2toHttp1ServerPost() throws Exception {
    setupClient(true);
    setupFrontBack(true, false);

    post(port(), false, HTTP_2);
  }

  @Test
  public void testHttp2toHttp2ServerGet() throws Exception {
    setupClient(true);
    setupFrontBack(true, true);

    get(port(), false, HTTP_2);
  }

  @Test
  public void testHttp1toHttp2ServerGet() throws Exception {
    setupClient(false);
    setupFrontBack(false, true);

    get(port(), false, HTTP_1_1);
  }

  @Test
  public void testHttp1toHttp2ServerPost() throws Exception {
    setupClient(false);
    setupFrontBack(false, true);

    post(port(), false, HTTP_1_1);
  }

  @Test
  public void testHttp2toHttp1ServerGetMany() throws Exception {
    setupClient(true);
    setupFrontBack(true, false);
    final int iterations = 8;
    requests(iterations, false);
  }

  @Test
  public void testHttp2toHttp2ServerGetMany() throws Exception {
    setupClient(true);
    setupFrontBack(true, true);
    final int iterations = 8;
    requests(iterations, false);
  }

  @Test
  public void testHttp2toHttp1ServerPostMany() throws Exception {
    setupClient(true);
    setupFrontBack(true, false);
    final int iterations = 8;
    requests(iterations, true);
  }

  @Test
  public void testHttp2toHttp2ServerPostMany() throws Exception {
    setupClient(true);
    setupFrontBack(true, true);
    final int iterations = 8;
    requests(iterations, true);
  }

  private void requests(int iterations, boolean post) throws Exception {
    final Queue<Response> responses = new ConcurrentLinkedDeque<>();
    final Waiter waiter = new Waiter();
    String url = url(port(), false);
    ExecutorService executorService = Executors.newFixedThreadPool(4);
    IntStream.range(0, iterations)
        .forEach(
            index -> {
              server.enqueue(buildResponse());
              executorService.submit(
                  () -> {
                    try {
                      Request.Builder request = new Request.Builder().url(url);
                      if (post) {
                        MediaType mediaType = MediaType.parse("text/plain");
                        RequestBody body = RequestBody.create(mediaType, "this is the post body");
                        request.post(body);
                      } else {
                        request.get();
                      }
                      Response response = client.newCall(request.build()).execute();
                      responses.offer(response);
                      waiter.resume();
                    } catch (IOException error) {
                      waiter.fail(error);
                    }
                  });
            });

    int seconds = 10;
    waiter.await(seconds, TimeUnit.SECONDS, iterations);
    assertEquals(iterations, responses.size());
    executorService.shutdown();
    executorService.awaitTermination(seconds, TimeUnit.SECONDS);
  }
}
