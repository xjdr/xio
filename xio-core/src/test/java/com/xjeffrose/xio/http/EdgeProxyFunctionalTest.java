package com.xjeffrose.xio.http;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.MutualAuthHandler;
import com.xjeffrose.xio.SSL.TlsAuthState;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.application.ApplicationConfig;
import com.xjeffrose.xio.application.ApplicationState;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.fixtures.JulBridge;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.experimental.Accessors;
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
public class EdgeProxyFunctionalTest extends Assert {

  @Accessors(fluent = true)
  @Getter
  public class RouteConfigs<T extends RouteConfig> {
    private final List<T> configs;

    public RouteConfigs(List<T> configs) {
      this.configs = configs;
    }

    public Stream<T> stream() {
      return configs.stream();
    }
  }

  @Accessors(fluent = true)
  @Getter
  public class RouteStates<T extends RouteState> {

    private final AtomicReference<ImmutableMap<String, T>> routes;

    public RouteStates(ImmutableMap<String, T> routes) {
      this.routes = new AtomicReference<>(routes);
    }

    public ImmutableMap<String, RouteState> routes() {
      return (ImmutableMap<String, RouteState>) routes.get();
    }
  }

  public class EdgeProxyConfig extends ApplicationConfig {
    private final RouteConfigs<ProxyRouteConfig> routeConfigs;

    // abstract authn handler
    // abstract authz handler
    // TODO(CK): replace this?
    private final ImmutableSet<String> allPermissions;

    public EdgeProxyConfig(Config config) {
      super(config);
      List<Config> routes = (List<Config>) config.getConfigList("routes");

      routeConfigs =
          new RouteConfigs<>(
              routes.stream().map(ProxyRouteConfig::new).collect(Collectors.toList()));
      allPermissions = null;
    }
  }

  public class EdgeProxyState extends ApplicationState {

    private final RouteStates<ProxyRouteState> routeStates;
    private final ProxyClientFactory clientFactory;

    public <T, K, U> Collector<T, ?, Map<K, U>> toLinkedMap(
        Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {

      return Collectors.toMap(
          keyMapper,
          valueMapper,
          (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
          },
          LinkedHashMap::new);
    }

    public EdgeProxyState(EdgeProxyConfig config) {
      super(config);
      clientFactory = new ProxyClientFactory(this);
      routeStates =
          new RouteStates<ProxyRouteState>(
              ImmutableMap.copyOf(
                  config
                      .routeConfigs
                      .stream()
                      .map(
                          (ProxyRouteConfig c) ->
                              new ProxyRouteState(this, c, new ProxyHandler(clientFactory, c)))
                      .collect(toLinkedMap(s -> s.path(), s -> s))));
    }

    public ImmutableMap<String, RouteState> routes() {
      return routeStates.routes();
    }
  }

  public class EdgeProxyApplicationBootstrap extends ApplicationBootstrap {
    EdgeProxyState state;

    public SmartHttpPipeline pipelineFragment() {
      return new SmartHttpPipeline() {

        @Override
        public ChannelHandler getTlsAuthenticationHandler() {
          return new MutualAuthHandler() {
            @Override
            public void peerIdentityEstablished(ChannelHandlerContext ctx, String identity) {
              if (!identity.equals(TlsAuthState.UNAUTHENTICATED)) {
                // GatekeeperClient.setResponse(ctx,
                // gatekeeperClient.authorize(identity.substring(3), allPermissions));
              }
            }
          };
        }

        @Override
        public ChannelHandler getApplicationRouter() {
          return new PipelineRouter(state.routes());
          // new ConfigurableHandler(new PipelineRouter(routes), RouteUpdateClass.class)
          // new ConfigurableInboundHandler(inbound handler class type, update class type)
          // new ConfigurableOutboundHandler(outbound handler class type, update class type)
          // new ConfigurableDuplexHandler(duplex handler class type, update class type)
        }

        @Override
        public ChannelHandler getAuthenticationHandler() {
          return null;
        }

        @Override
        public ChannelHandler getAuthorizationHandler() {
          return null;
        }
      };
    }

    public Application build() {
      addServer("main", bs -> bs.addToPipeline(pipelineFragment()));
      return super.build();
    }

    private EdgeProxyApplicationBootstrap(EdgeProxyState state) {
      super(state);
      this.state = state;
    }

    public EdgeProxyApplicationBootstrap() {
      this(new EdgeProxyState(new EdgeProxyConfig(config())));
    }
  }

  @BeforeClass
  public static void setupJul() {
    JulBridge.initialize();
  }

  OkHttpClient client;
  MockWebServer server;
  Application edgeProxy;

  @Rule public TestName testName = new TestName();

  private Config config() {
    // TODO(CK): this creates global state across tests we should do something smarter
    System.setProperty("xio.baseClient.remotePort", Integer.toString(server.getPort()));
    System.setProperty("xio.proxyRouteTemplate.proxyPath", "/");
    ConfigFactory.invalidateCaches();
    Config root = ConfigFactory.load();
    return root.getConfig("xio.edgeProxyApplication");
  }

  @Before
  public void setUp() throws Exception {
    log.debug("Test: " + testName.getMethodName());

    client =
        OkHttpUnsafe.getUnsafeClient()
            .newBuilder()
            .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build();

    TlsConfig tlsConfig =
        TlsConfig.fromConfig("xio.h2BackendServer.settings.tls", ConfigFactory.load());
    server = OkHttpUnsafe.getSslMockWebServer(tlsConfig);
    server.setProtocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1));
    server.start();
  }

  @After
  public void tearDown() throws Exception {
    client.connectionPool().evictAll();
    if (edgeProxy != null) {
      edgeProxy.close();
    }
    server.close();
  }

  int port() {
    return edgeProxy.instrumentation("main").boundAddress().getPort();
  }

  String url(String prefix, int port) {
    StringBuilder path =
        new StringBuilder("https://")
            .append("127.0.0.1")
            .append(":")
            .append(port)
            .append(prefix)
            .append("/hello/world");
    return path.toString();
  }

  MockResponse buildResponse() {
    return new MockResponse().setBody("hello, world").setSocketPolicy(SocketPolicy.KEEP_OPEN);
  }

  void get(String prefix, int port) throws Exception {
    String url = url(prefix, port);
    Request request = new Request.Builder().url(url).build();

    server.enqueue(buildResponse());
    Response response = client.newCall(request).execute();
    assertEquals(200, response.code());

    RecordedRequest servedRequest = server.takeRequest();
    assertEquals("/hello/world", servedRequest.getRequestUrl().encodedPath());
  }

  void post(String prefix, int port) throws Exception {
    String url = url(prefix, port);
    MediaType mediaType = MediaType.parse("text/plain");
    RequestBody body = RequestBody.create(mediaType, "this is the post body");
    Request request = new Request.Builder().url(url).post(body).build();

    server.enqueue(buildResponse());
    Response response = client.newCall(request).execute();
    assertEquals(200, response.code());

    RecordedRequest servedRequest = server.takeRequest();
    assertEquals("/hello/world", servedRequest.getRequestUrl().encodedPath());
    assertEquals("this is the post body", servedRequest.getBody().readUtf8());
  }

  @Test
  public void sanityCheckHttpGet() throws Exception {
    get("", server.getPort());
  }

  @Test
  public void sanityCheckHttpPost() throws Exception {
    post("", server.getPort());
  }

  @Test
  public void testHttpGet() throws Exception {
    edgeProxy = new EdgeProxyApplicationBootstrap().build();
    get("/valid-path", port());
  }

  @Test
  public void testHttpPost() throws Exception {
    edgeProxy = new EdgeProxyApplicationBootstrap().build();
    post("/valid-path", port());
  }

  @Test
  public void testConfigReload() {}
}
