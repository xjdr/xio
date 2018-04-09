package com.xjeffrose.xio.client.http;

import com.google.common.util.concurrent.Uninterruptibles;
import com.xjeffrose.xio.SSL.TlsConfig;
import com.xjeffrose.xio.client.ChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.client.Http;
import com.xjeffrose.xio.client.XioClientBootstrap;
import com.xjeffrose.xio.fixtures.JulBridge;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.logging.*;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HttpClientFunctionalTest extends Assert {

  @BeforeClass
  public static void setupJul() {
    JulBridge.initialize();
  }

  MockWebServer server;

  static Logger disableJavaLogging() {
    Logger logger = Logger.getLogger("okhttp3.mockwebserver.MockWebServer");
    logger.setLevel(Level.WARNING);
    return logger;
  }

  Logger hush = disableJavaLogging();

  @Before
  public void setUp() throws Exception {
    TlsConfig tlsConfig = TlsConfig.fromConfig("xio.h1TestClient.settings.tls");
    server = OkHttpUnsafe.getSslMockWebServer(tlsConfig);

    // Schedule some responses.
    server.enqueue(new MockResponse().setBody("hello, world!"));
    server.start();
  }

  @After
  public void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  public void testSslRequest() throws InterruptedException {
    CountDownLatch receivedResponse = new CountDownLatch(2);

    final ConcurrentLinkedQueue<HttpObject> responses = new ConcurrentLinkedQueue<>();
    ChannelHandler responseHandler =
        new SimpleChannelInboundHandler<HttpObject>() {
          @Override
          protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
            responses.add(msg);
            receivedResponse.countDown();
          }
        };
    ClientConfig config = ClientConfig.fromConfig("xio.h1TestClient");
    XioClientBootstrap bootstrap =
        new XioClientBootstrap(config)
            .channelConfig(ChannelConfiguration.clientConfig(1))
            .handler(responseHandler);
    HttpClientBuilder builder = new HttpClientBuilder(bootstrap);
    URL url = server.url("/hello-world").url();
    HttpClient client = builder.endpointForUrl(url).build();

    client.write(Http.get("/hello-world"));

    Uninterruptibles.awaitUninterruptibly(receivedResponse); // block

    // check request
    RecordedRequest request1 = server.takeRequest();
    assertEquals("/hello-world", request1.getPath());

    // check response
    assertEquals(HttpResponseStatus.OK, ((HttpResponse) responses.poll()).status());
  }
}
