package com.xjeffrose.xio.proxy;

import static com.xjeffrose.xio.test.OkHttpUnsafe.getKeyManagers;
import static okhttp3.Protocol.HTTP_1_1;
import static okhttp3.Protocol.HTTP_2;

import com.google.common.collect.Streams;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.test.JulBridge;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import com.xjeffrose.xio.tls.TlsConfig;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
public class ReverseProxyFunctionalTest extends Assert {

  @BeforeClass
  public static void setupJul() {
    JulBridge.initialize();
  }

  private static final int NUM_REQUESTS = 10;

  List<OkHttpClient> clients;
  Config config;
  MockWebServer backEnd1;
  MockWebServer backEnd2;
  ReverseProxyServer reverseProxy;

  @Rule public TestName testName = new TestName();

  @Before
  public void setupCommon() {
    config = ConfigFactory.load();
    log.debug("Test: " + testName.getMethodName());
  }

  void setupBack(boolean h2) throws Exception {
    setupBack(h2, false);
  }

  void setupBack(boolean h2, boolean isSecondServer) throws Exception {
    String back = h2 ? "h2" : "h1";

    TlsConfig tlsConfig =
        TlsConfig.builderFrom(config.getConfig("xio." + back + "BackendServer.settings.tls"))
            .build();
    List<Protocol> protocols;
    if (h2) {
      protocols = Arrays.asList(HTTP_2, HTTP_1_1);
    } else {
      protocols = Collections.singletonList(HTTP_1_1);
    }

    MockWebServer server =
        OkHttpUnsafe.getSslMockWebServer(
            getKeyManagers(tlsConfig.getPrivateKey(), tlsConfig.getCertificateAndChain()));
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
    if (isSecondServer) {
      this.backEnd2 = server;
      server.start(8442);
    } else {
      this.backEnd1 = server;
      server.start(8443);
    }
  }

  void setupFrontBack(boolean h2Front, boolean h2Back) throws Exception {
    setupBack(h2Back);
    final String proxyConfig;
    if (h2Front) {
      proxyConfig = "xio.h2ReverseProxy";
    } else {
      proxyConfig = "xio.h1ReverseProxy";
    }
    reverseProxy = new ReverseProxyServer(proxyConfig, "xio.testProxyRoute");
    reverseProxy.start(config);
  }

  void setupFront(boolean h2Front, String proxyRouteConfigs) {
    final String proxyConfig;
    if (h2Front) {
      proxyConfig = "xio.h2ReverseProxy";
    } else {
      proxyConfig = "xio.h1ReverseProxy";
    }
    reverseProxy = new ReverseProxyServer(proxyConfig, proxyRouteConfigs);
    reverseProxy.start(config);
  }

  private void setupClient(int count, boolean h2) throws Exception {
    clients =
        IntStream.range(0, count)
            .mapToObj(
                index -> {
                  try {
                    if (h2) {
                      return OkHttpUnsafe.getUnsafeClient()
                          .newBuilder()
                          .protocols(Arrays.asList(HTTP_2, HTTP_1_1))
                          .build();
                    } else {
                      return OkHttpUnsafe.getUnsafeClient()
                          .newBuilder()
                          .protocols(Collections.singletonList(HTTP_1_1))
                          .build();
                    }
                  } catch (Exception e) {
                    return null;
                  }
                })
            .collect(Collectors.toList());
  }

  private void aggressivelyCloseClients() throws Exception {
    clients.forEach(
        client -> {
          client.dispatcher().executorService().shutdown();
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
              .blockingSubscribe();
        });
  }

  @After
  public void tearDown() throws Exception {
    aggressivelyCloseClients();
    if (reverseProxy != null) {
      reverseProxy.stop();
    }
    backEnd1.close();
    if (backEnd2 != null) {
      backEnd2.close();
    }
  }

  int proxyPort() {
    return reverseProxy.port();
  }

  String url(int port, String path) {
    return "https://" + "127.0.0.1" + ":" + port + path;
  }

  void get(
      int port,
      Protocol expectedProtocol,
      String path,
      String expectedPath,
      MockWebServer expectedBackend)
      throws Exception {
    String url = url(port, path);
    Request request = new Request.Builder().url(url).build();

    Response response = clients.get(0).newCall(request).execute();
    response.close();
    assertEquals(expectedProtocol, response.protocol());

    RecordedRequest servedRequest = expectedBackend.takeRequest();
    assertEquals(expectedPath, servedRequest.getRequestUrl().encodedPath());
  }

  void post(
      int port,
      Protocol expectedProtocol,
      String path,
      String expectedPath,
      MockWebServer expectedBackend)
      throws Exception {
    String url = url(port, path);
    MediaType mediaType = MediaType.parse("text/plain");
    RequestBody body = RequestBody.create(mediaType, "this is the post body");
    Request request = new Request.Builder().url(url).post(body).build();

    Response response = clients.get(0).newCall(request).execute();
    response.close();
    assertEquals("unexpected client response protocol", expectedProtocol, response.protocol());

    RecordedRequest servedRequest = expectedBackend.takeRequest();
    assertEquals(expectedPath, servedRequest.getRequestUrl().encodedPath());
    assertEquals("this is the post body", servedRequest.getBody().readUtf8());
  }

  private void assertProxiedRequests(int count) {
    if (reverseProxy != null) {
      assertEquals(count, reverseProxy.getRequestCount());
    }
  }

  @Test
  public void sanityCheckHttp1Get() throws Exception {
    setupClient(1, false);
    setupBack(false);

    get(backEnd1.getPort(), HTTP_1_1, "/foo", "/foo", backEnd1);
    assertEquals(1, backEnd1.getRequestCount());
    assertProxiedRequests(0);
  }

  @Test
  public void sanityCheckHttp1Post() throws Exception {
    setupClient(1, false);
    setupBack(false);

    post(backEnd1.getPort(), HTTP_1_1, "/ifoo/", "/ifoo/", backEnd1);
    assertEquals(1, backEnd1.getRequestCount());
    assertProxiedRequests(0);
  }

  @Test
  public void sanityCheckHttp2Get() throws Exception {
    setupClient(1, true);
    setupBack(true);

    get(backEnd1.getPort(), HTTP_2, "/ifoo/", "/ifoo/", backEnd1);
    assertEquals(1, backEnd1.getRequestCount());
    assertProxiedRequests(0);
  }

  @Test
  public void sanityCheckHttp2Post() throws Exception {
    setupClient(1, true);
    setupBack(true);

    post(backEnd1.getPort(), HTTP_2, "/ifoo/", "/ifoo/", backEnd1);
    assertEquals(1, backEnd1.getRequestCount());
    assertProxiedRequests(0);
  }

  @Test
  public void testHttp2toHttp1ServerGet() throws Exception {
    setupClient(1, true);
    setupFrontBack(true, false);

    get(proxyPort(), HTTP_2, "/foo/", "/ifoo/", backEnd1);
    assertEquals(1, backEnd1.getRequestCount());
    assertProxiedRequests(1);
  }

  @Test
  public void testMultipleBacksMixedGET() throws Exception {
    // given 1 client
    setupClient(1, true);

    // and 1 proxy
    setupFront(true, "xio.testProxyRoute,xio.testProxyRoute2Foo");

    // and 2 backends
    setupBack(false);
    setupBack(true, true);

    // when routes corresponding to each back end is queried
    // then the appropriate backend is reached
    get(proxyPort(), HTTP_2, "/foo/", "/ifoo/", backEnd1);
    get(proxyPort(), HTTP_2, "/bar/", "/ibar/", backEnd2);
    assertEquals(1, backEnd1.getRequestCount());
    assertEquals(1, backEnd2.getRequestCount());
    assertProxiedRequests(2);
  }

  @Test
  public void testHttp2toHttp1ServerPost() throws Exception {
    setupClient(1, true);
    setupFrontBack(true, false);

    post(proxyPort(), HTTP_2, "/foo/", "/ifoo/", backEnd1);
    assertEquals(1, backEnd1.getRequestCount());
    assertProxiedRequests(1);
  }

  @Test
  public void testHttp2toHttp2ServerGet() throws Exception {
    setupClient(1, true);
    setupFrontBack(true, true);

    get(proxyPort(), HTTP_2, "/foo/", "/ifoo/", backEnd1);
    assertEquals(1, backEnd1.getRequestCount());
    assertProxiedRequests(1);
  }

  @Test
  public void testHttp1toHttp2ServerGet() throws Exception {
    setupClient(1, false);
    setupFrontBack(false, true);

    get(proxyPort(), HTTP_1_1, "/foo/", "/ifoo/", backEnd1);
    assertEquals(1, backEnd1.getRequestCount());
    assertProxiedRequests(1);
  }

  @Test
  public void testHttp1toHttp2ServerPost() throws Exception {
    setupClient(1, false);
    setupFrontBack(false, true);

    post(proxyPort(), HTTP_1_1, "/foo/", "/ifoo/", backEnd1);
    assertEquals(1, backEnd1.getRequestCount());
    assertProxiedRequests(1);
  }

  @Test
  public void testHttp1toHttp2ServerPostMany() throws Exception {
    setupClient(NUM_REQUESTS, false);
    setupFrontBack(false, true);

    verify(multipleAsyncRequests(true).blockingIterable());
    assertEquals(NUM_REQUESTS * 2, backEnd1.getRequestCount());
    assertProxiedRequests(NUM_REQUESTS * 2);
  }

  @Test
  public void testHttp1toHttp1ServerGetMany() throws Exception {
    setupClient(NUM_REQUESTS, true);
    setupFrontBack(false, false);

    verify(multipleAsyncRequests(false).blockingIterable());
    assertEquals(NUM_REQUESTS * 2, backEnd1.getRequestCount());
    assertProxiedRequests(NUM_REQUESTS * 2);
  }

  @Test
  public void testHttp1toHttp2ServerGetMany() throws Exception {
    setupClient(NUM_REQUESTS, false);
    setupFrontBack(false, true);

    verify(multipleAsyncRequests(false).blockingIterable());
    assertEquals(NUM_REQUESTS * 2, backEnd1.getRequestCount());
    assertProxiedRequests(NUM_REQUESTS * 2);
  }

  @Test
  public void testHttp1toHttp1ServerPostMany() throws Exception {
    setupClient(NUM_REQUESTS, true);
    setupFrontBack(false, false);

    verify(multipleAsyncRequests(true).blockingIterable());
    assertEquals(NUM_REQUESTS * 2, backEnd1.getRequestCount());
    assertProxiedRequests(NUM_REQUESTS * 2);
  }

  @Test
  public void testHttp2toHttp1ServerGetMany() throws Exception {
    setupClient(NUM_REQUESTS, true);
    setupFrontBack(true, false);

    verify(multipleAsyncRequests(false).blockingIterable());
    assertEquals(NUM_REQUESTS * 2, backEnd1.getRequestCount());
    assertProxiedRequests(NUM_REQUESTS * 2);
  }

  @Test
  public void testHttp2toHttp2ServerGetMany() throws Exception {
    setupClient(NUM_REQUESTS, true);
    setupFrontBack(true, true);

    verify(multipleAsyncRequests(false).blockingIterable());
    assertEquals(20, backEnd1.getRequestCount());
    assertProxiedRequests(20);
  }

  @Test
  public void testHttp2toHttp1ServerPostMany() throws Exception {
    setupClient(NUM_REQUESTS, true);
    setupFrontBack(true, false);

    verify(multipleAsyncRequests(true).blockingIterable());
    assertEquals(20, backEnd1.getRequestCount());
    assertProxiedRequests(20);
  }

  @Test
  public void testHttp2toHttp2ServerPostMany() throws Exception {
    setupClient(NUM_REQUESTS, true);
    setupFrontBack(true, true);

    verify(multipleAsyncRequests(true).blockingIterable());
    assertEquals(NUM_REQUESTS * 2, backEnd1.getRequestCount());
    assertProxiedRequests(NUM_REQUESTS * 2);
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
    String url = url(proxyPort(), "/foo/");
    return Observable.merge(
        Observable.fromIterable(() -> clients.iterator())
            .map(
                client -> requestAsync(client, url, post, clients.indexOf(client)).toObservable()));
  }

  private Single<IndexResponse> requestAsync(
      OkHttpClient client, String url, boolean post, int xIndex) {
    return Single.<IndexResponse>create(
            emitter -> {
              log.debug("making request index {}", xIndex);
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
              log.debug("response {} index {}", response, xIndex);
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
