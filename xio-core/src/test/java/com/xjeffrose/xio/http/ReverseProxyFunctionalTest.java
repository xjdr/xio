package com.xjeffrose.xio.http;

import static com.xjeffrose.xio.helpers.TlsHelper.getKeyManagers;
import static okhttp3.Protocol.HTTP_1_1;
import static okhttp3.Protocol.HTTP_2;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.core.SocketAddressHelper;
import com.xjeffrose.xio.fixtures.JulBridge;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import io.netty.channel.ChannelHandler;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.*;
import okhttp3.mockwebserver.Dispatcher;
import org.junit.*;
import org.junit.rules.TestName;

@Slf4j
@Ignore("todo: fix after gradle maven publish")
public class ReverseProxyFunctionalTest extends Assert {

  @BeforeClass
  public static void setupJul() {
    JulBridge.initialize();
  }

  private static final int NUM_REQUESTS = 10;

  OkHttpClient client;
  Config config;
  ApplicationState applicationState;
  Application reverseProxy;
  MockWebServer server;

  static Application setupReverseProxy(ApplicationState state, ProxyRouteConfig proxyConfig) {
    ClientFactory factory = new ProxyClientFactory(state);
    return new ApplicationBootstrap(state.config())
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
    server.setDispatcher(
        new Dispatcher() {
          @Override
          public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            String index = Optional.ofNullable(request.getHeader("x_index")).orElse("unknown");
            return new MockResponse()
                .addHeader("x_index", index)
                .setBody("hello, world")
                .setSocketPolicy(SocketPolicy.KEEP_OPEN);
          }
        });
    server.start();
  }

  void setupFrontBack(boolean h2Front, boolean h2Back) throws Exception {
    setupBack(h2Back);

    String front = h2Front ? "h2" : "h1";
    applicationState =
        new ApplicationState(ApplicationConfig.fromConfig("xio." + front + "ReverseProxy", config));
    // TODO(CK): this creates global state across tests we should do something smarter
    System.setProperty("xio.baseClient.remotePort", Integer.toString(server.getPort()));
    System.setProperty("xio.testProxyRoute.proxyPath", "/hello/");
    ConfigFactory.invalidateCaches();
    Config root = ConfigFactory.load();
    ProxyRouteConfig proxyConfig = new ProxyRouteConfig(root.getConfig("xio.testProxyRoute"));

    reverseProxy = setupReverseProxy(applicationState, proxyConfig);
  }

  private void setupClient(boolean h2) throws Exception {
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

  private void aggressivelyCloseClient() throws Exception {
    client.dispatcher().executorService().shutdown();
    long pollMs =
        Observable.interval(100, TimeUnit.MILLISECONDS)
                .takeUntil(
                    i -> {
                      boolean canEvict =
                          client.connectionPool().idleConnectionCount()
                              == client.connectionPool().connectionCount();
                      client.connectionPool().evictAll();
                      return canEvict;
                    })
                .timeout(15, TimeUnit.SECONDS)
                .blockingLast()
            * 100;
    log.warn("polled client shutdown for {}ms", pollMs);
  }

  @After
  public void tearDown() throws Exception {
    aggressivelyCloseClient();
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

  void get(int port, boolean sanity, Protocol expectedProtocol) throws Exception {
    String url = url(port, sanity);
    Request request = new Request.Builder().url(url).build();

    Response response = client.newCall(request).execute();
    response.close();
    assertEquals(expectedProtocol, response.protocol());

    RecordedRequest servedRequest = server.takeRequest();
    assertEquals("/hello/", servedRequest.getRequestUrl().encodedPath());
  }

  void post(int port, boolean sanity, Protocol expectedProtocol) throws Exception {
    String url = url(port, sanity);
    MediaType mediaType = MediaType.parse("text/plain");
    RequestBody body = RequestBody.create(mediaType, "this is the post body");
    Request request = new Request.Builder().url(url).post(body).build();

    Response response = client.newCall(request).execute();
    response.close();
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
  public void testHttp1toHttp2ServerPostMany() throws Exception {
    setupClient(false);
    setupFrontBack(false, true);

    verify(multipleAsyncRequests(true).blockingIterable());
  }

  @Test
  public void testHttp1toHttp1ServerGetMany() throws Exception {
    setupClient(true);
    setupFrontBack(false, false);

    verify(multipleAsyncRequests(false).blockingIterable());
  }

  @Test
  public void testHttp1toHttp2ServerGetMany() throws Exception {
    setupClient(false);
    setupFrontBack(false, true);

    verify(multipleAsyncRequests(false).blockingIterable());
  }

  @Test
  public void testHttp1toHttp1ServerPostMany() throws Exception {
    setupClient(true);
    setupFrontBack(false, false);

    verify(multipleAsyncRequests(true).blockingIterable());
  }

  @Test
  public void testHttp2toHttp1ServerGetMany() throws Exception {
    setupClient(true);
    setupFrontBack(true, false);

    verify(multipleAsyncRequests(false).blockingIterable());
  }

  @Test
  public void testHttp2toHttp2ServerGetMany() throws Exception {
    setupClient(true);
    setupFrontBack(true, true);

    verify(multipleAsyncRequests(false).blockingIterable());
  }

  @Test
  public void testHttp2toHttp1ServerPostMany() throws Exception {
    setupClient(true);
    setupFrontBack(true, false);

    verify(multipleAsyncRequests(true).blockingIterable());
  }

  @Test
  public void testHttp2toHttp2ServerPostMany() throws Exception {
    setupClient(true);
    setupFrontBack(true, true);

    verify(multipleAsyncRequests(true).blockingIterable());
  }

  private void verify(Iterable<IndexResponse> responses) {
    // verifies that the correct number of responses occurred and that each response corresponds wit the request by
    // checking the "x_index" header value
    assertEquals(NUM_REQUESTS, Streams.stream(responses).count());
    responses.forEach(
        pair -> {
          String index = pair.response.header("x_index");
          assertNotNull(index);
          assertEquals(pair.xIndex.toString(), index);
        });
  }

  private Observable<IndexResponse> multipleAsyncRequests(boolean post) {
    return Observable.merge(
        Observable.fromIterable(() -> IntStream.range(0, NUM_REQUESTS).iterator())
            .map(index -> requestAsync(post, index).toObservable()));
  }

  private Single<IndexResponse> requestAsync(boolean post, int xIndex) {
    String url = url(port(), false);
    return Single.<IndexResponse>create(
            emitter -> {
              Request.Builder request =
                  new Request.Builder().header("x_index", String.valueOf(xIndex)).url(url);
              if (post) {
                MediaType mediaType = MediaType.parse("text/plain");
                RequestBody body = RequestBody.create(mediaType, "this is the post body");
                request.post(body);
              } else {
                request.get();
              }
              Response response = client.newCall(request.build()).execute();
              log.debug("response {}", response);
              response.close();
              emitter.onSuccess(new IndexResponse(xIndex, response));
            })
        .subscribeOn(Schedulers.io());
  }

  static class IndexResponse {
    private final Integer xIndex;
    private final Response response;

    IndexResponse(Integer first, Response response) {
      this.xIndex = first;
      this.response = response;
    }
  }
}
