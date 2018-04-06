package com.xjeffrose.xio.tracing;

import static io.netty.handler.codec.http.HttpMethod.*;
import static org.junit.Assert.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.fixtures.JulBridge;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import com.xjeffrose.xio.http.*;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.*;
import lombok.NonNull;
import lombok.val;
import okhttp3.Protocol;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.apache.commons.codec.binary.Hex;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import zipkin.Span;
import zipkin.reporter.Reporter;

// TODO(CK): These brave integration tests are flaky and stall out sometimes
// Turn them back on when they are fixed
public class HttpClientTracingHandlerIntegrationTest { // extends ITHttpClient<XioClient> {
  String expectedResponse = "hello, world";
  Response response;
  List<Span> reportedSpans;
  Logger hush = disableJavaLogging();
  CompletableFuture<Response> local = new CompletableFuture<Response>();
  MockWebServer server;

  private class FakeTracer extends XioTracing {
    public FakeTracer(Config config) {
      super(config);
    }

    @Override
    Reporter<zipkin.Span> buildReporter(@NonNull String zipkinUrl) {
      return new Reporter<Span>() {
        @Override
        public void report(Span span) {
          reportedSpans.add(span);
        }
      };
    }
  }

  @BeforeClass
  public static void setupJul() {
    JulBridge.initialize();
  }

  @Rule
  public TestWatcher testWatcher =
      new TestWatcher() {
        @Override
        protected void starting(final Description description) {
          String methodName = description.getMethodName();
          String className = description.getClassName();
          className = className.substring(className.lastIndexOf('.') + 1);
          // System.out.println("Starting JUnit-test: " + className + " " + methodName);
        }
      };

  static Logger disableJavaLogging() {
    Logger logger = Logger.getLogger("okhttp3.mockwebserver.MockWebServer");
    logger.setLevel(Level.WARNING);
    return logger;
  }

  public class ApplicationHandler extends SimpleChannelInboundHandler<Response> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Response object) throws Exception {
      if (object.endOfStream()) {
        response = object;
        local.complete(response);
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      local.completeExceptionally(cause);
    }
  }

  private Config config() {
    // TODO(CK): this creates global state across tests we should do something smarter
    System.setProperty("xio.baseClient.remotePort", Integer.toString(server.getPort()));
    System.setProperty("xio.proxyRouteTemplate.proxyPath", "/");
    ConfigFactory.invalidateCaches();
    Config root = ConfigFactory.load();
    return root.getConfig("xio.tracingHandlerClientIntegrationTest");
  }

  Client newClient(int port, XioTracing tracing) {
    val channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    val clientConfig = new ClientConfig(ConfigFactory.load().getConfig("xio.baseClient"));
    val clientState = new ClientState(channelConfig, clientConfig);

    val client = new Client(clientState, () -> new ApplicationHandler(), tracing);

    return client;
  }

  MockResponse buildResponse() {
    return new MockResponse().setBody(expectedResponse).setSocketPolicy(SocketPolicy.KEEP_OPEN);
  }

  @Before
  public void setUp() throws Exception {
    reportedSpans = new ArrayList<Span>();

    TlsConfig tlsConfig =
        TlsConfig.fromConfig("xio.h2BackendServer.settings.tls", ConfigFactory.load());
    server = OkHttpUnsafe.getSslMockWebServer(tlsConfig);
    server.setProtocols(Arrays.asList(Protocol.HTTP_1_1));
    server.enqueue(buildResponse());
    server.start();
  }

  @After
  public void tearDown() throws Exception {
    server.close();
  }

  @Test
  public void testOutBoundAndInboundSpan() throws Exception {
    val client = newClient(server.getPort(), new FakeTracer(config()));

    val request =
        DefaultStreamingRequest.builder()
            .method(GET)
            .path("/v1/authinit")
            .host("127.0.0.1" + ":" + server.getPort())
            .build();

    val writeFuture = client.write(request);
    // We wait on the local future because this signals the full roundtrip between outbound and
    // return trip from
    // the Application Handler out and then back in.
    local.get();
    assertEquals(reportedSpans.size(), 1);

    val responseHex = ByteBufUtil.hexDump(((StreamingData) response).content());
    byte[] bytes = Hex.decodeHex(responseHex.toCharArray());
    assertEquals(expectedResponse, new String(bytes, "UTF-8"));
  }
  /*
  // @Override
  @Test(expected = ComparisonFailure.class)
  public void redirect() throws Exception {
    throw new AssumptionViolatedException("client does not support redirect");
  }

  // @Override
  @Test(expected = ComparisonFailure.class)
  public void addsStatusCodeWhenNotOk() throws Exception {
    throw new AssumptionViolatedException("test is flaky");
  }

  // @Override
  @Test(expected = ComparisonFailure.class)
  public void httpPathTagExcludesQueryParams() throws Exception {
    throw new AssumptionViolatedException("test is flaky");
  }
  */
}
