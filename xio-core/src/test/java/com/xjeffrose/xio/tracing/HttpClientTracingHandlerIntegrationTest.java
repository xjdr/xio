package com.xjeffrose.xio.tracing;

import static com.xjeffrose.xio.helpers.TlsHelper.getKeyManagers;
import static io.netty.handler.codec.http.HttpMethod.*;
import static org.junit.Assert.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.http.*;
import com.xjeffrose.xio.test.JulBridge;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import java.util.ArrayList;
import java.util.Collections;
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
import zipkin2.Span;
import zipkin2.reporter.Reporter;

public class HttpClientTracingHandlerIntegrationTest {
  private String expectedResponse = "hello, world";
  private Response response;
  private List<Span> reportedSpans;
  private CompletableFuture<Response> local = new CompletableFuture<>();
  private MockWebServer server;

  private class FakeTracer extends XioTracing {
    FakeTracer(Config config) {
      super(config);
    }

    @Override
    Reporter<zipkin2.Span> buildReporter(@NonNull String zipkinUrl) {
      return span -> reportedSpans.add(span);
    }
  }

  private static void disableJavaLogging() {
    Logger.getLogger("okhttp3.mockwebserver.MockWebServer").setLevel(Level.WARNING);
  }

  public class ApplicationHandler extends SimpleChannelInboundHandler<Response> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Response object) throws Exception {
      if (object.endOfMessage()) {
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
    System.setProperty(
        "xio.baseClient.remotePort",
        Integer.toString(
            server
                .getPort())); // Maybe just pick a port, then run the test by starting the mock web server with that port?
    System.setProperty("xio.proxyRouteTemplate.proxyPath", "/");
    ConfigFactory.invalidateCaches();
    Config root = ConfigFactory.load();
    return root.getConfig("xio.tracingHandlerClientIntegrationTest");
  }

  private Client newClient(XioTracing tracing) {
    val channelConfig = ChannelConfiguration.clientConfig(1, "client-tracing-test-worker");
    val clientConfig = ClientConfig.from(ConfigFactory.load().getConfig("xio.baseClient"));
    val clientState = new ClientState(channelConfig, clientConfig);
    ClientChannelInitializer clientChannelInit =
        new ClientChannelInitializer(clientState, ApplicationHandler::new, tracing);
    ClientConnectionManager connManager =
        new ClientConnectionManager(clientState, clientChannelInit);

    return new Client(clientState, connManager);
  }

  private MockResponse buildResponse() {
    return new MockResponse().setBody(expectedResponse).setSocketPolicy(SocketPolicy.KEEP_OPEN);
  }

  @BeforeClass
  public static void setupJul() {
    disableJavaLogging();
    JulBridge.initialize();
  }

  @Before
  public void setUp() throws Exception {
    reportedSpans = new ArrayList<>();

    TlsConfig tlsConfig =
        TlsConfig.fromConfig("xio.h2BackendServer.settings.tls", ConfigFactory.load());
    server = OkHttpUnsafe.getSslMockWebServer(getKeyManagers(tlsConfig));
    server.setProtocols(Collections.singletonList(Protocol.HTTP_1_1));
    server.enqueue(buildResponse());
    server.start();
  }

  @After
  public void tearDown() throws Exception {
    server.close();
  }

  @Test
  public void testOutBoundAndInboundSpan() throws Exception {
    val client = newClient(new FakeTracer(config()));

    val request =
        DefaultSegmentedRequest.builder()
            .method(GET)
            .path("/v1/authinit")
            .host("127.0.0.1" + ":" + server.getPort())
            .build();

    client.write(request);
    // We wait on the local future because this signals the full roundtrip between outbound and
    // return trip from the Application Handler out and then back in.
    local.get();
    assertEquals(reportedSpans.size(), 1);

    val responseHex = ByteBufUtil.hexDump(((SegmentedData) response).content());
    byte[] bytes = Hex.decodeHex(responseHex.toCharArray());
    assertEquals(expectedResponse, new String(bytes, "UTF-8"));
  }
}
