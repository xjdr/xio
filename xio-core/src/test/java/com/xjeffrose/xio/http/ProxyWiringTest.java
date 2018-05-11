package com.xjeffrose.xio.http;

import static com.xjeffrose.xio.helpers.TlsHelper.getKeyManagers;

import com.google.common.collect.ImmutableMap;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.application.Application;
import com.xjeffrose.xio.bootstrap.ApplicationBootstrap;
import com.xjeffrose.xio.helpers.ProxyPipelineRequestHandler;
import com.xjeffrose.xio.pipeline.SmartHttpPipeline;
import com.xjeffrose.xio.pipeline.XioPipelineFragment;
import com.xjeffrose.xio.test.JulBridge;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import io.netty.channel.ChannelHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ProxyWiringTest extends Assert {

  @BeforeClass
  public static void setupJul() {
    JulBridge.initialize();
  }

  private OkHttpClient client;
  private MockWebServer server;

  @Before
  public void setUp() throws Exception {
    TlsConfig tlsConfig = TlsConfig.fromConfig("xio.testServer.settings.tls");
    client = OkHttpUnsafe.getUnsafeClient();
    server = OkHttpUnsafe.getSslMockWebServer(getKeyManagers(tlsConfig));
    server.start();
  }

  @After
  public void tearDown() throws Exception {
    server.close();
  }

  @Test
  public void sanityCheck() throws Exception {
    server.enqueue(new MockResponse().setBody("hello, world!"));
    InetSocketAddress address = new InetSocketAddress(server.getPort());

    Request request = buildRequest(address);
    Response response = get(request);

    RecordedRequest servedRequest = server.takeRequest();
    assertEquals("/api/v1/fives/hand/slap", servedRequest.getRequestUrl().encodedPath());
  }

  private XioPipelineFragment proxyFragment(InetSocketAddress boundAddress) {
    return new SmartHttpPipeline() {
      @Override
      public ChannelHandler getApplicationRouter() {
        return new PipelineRouter(
            ImmutableMap.of(), new ProxyPipelineRequestHandler(boundAddress, true));
      }
    };
  }

  @Test
  public void testProxy() throws Exception {
    InetSocketAddress proxiedAddress = new InetSocketAddress("127.0.0.1", server.getPort());
    Application application =
        new ApplicationBootstrap("xio.proxyApplication")
            .addServer("proxyServer", (bs) -> bs.addToPipeline(proxyFragment(proxiedAddress)))
            .build();

    Dispatcher dispatcher =
        new Dispatcher() {
          @Override
          public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            return new MockResponse().setBody("['I am a json response']");
          }
        };
    server.setDispatcher(dispatcher);

    InetSocketAddress proxy = application.instrumentation("proxyServer").boundAddress();

    Request request = buildRequest(proxy);
    Response response = get(request);

    RecordedRequest servedRequest = server.takeRequest();
    application.close();
    assertEquals("/api/v1/fives/hand/slap", servedRequest.getRequestUrl().encodedPath());
  }

  protected Request buildRequest(InetSocketAddress address) throws IOException {
    StringBuilder path =
        new StringBuilder("https://")
            .append("127.0.0.1")
            .append(":")
            .append(address.getPort())
            .append("/api/v1/fives/hand/slap");
    return new Request.Builder().url(path.toString()).build();
  }

  protected Response get(Request request) throws IOException {
    try (Response response = client.newCall(request).execute()) {
      if (response.code() == 404) {
        throw new AssumptionViolatedException(request.url().encodedPath() + " not supported");
      }
      return response;
    }
  }
}
