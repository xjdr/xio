package com.xjeffrose.xio.http;

import static com.xjeffrose.xio.helpers.TlsHelper.getKeyManagers;
import static io.netty.handler.codec.http.HttpMethod.GET;

import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.bootstrap.ChannelConfiguration;
import com.xjeffrose.xio.bootstrap.ClientChannelConfiguration;
import com.xjeffrose.xio.client.ClientConfig;
import com.xjeffrose.xio.test.OkHttpUnsafe;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import okhttp3.Protocol;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientProxiedRequestTest extends Assert {

  private Client client;
  private MockWebServer mockWebServer;
  private CountDownLatch latch;
  private TestHandler testHandler;

  @Before
  public void beforeEach() throws Exception {
    mockWebServer = OkHttpUnsafe.getSslMockWebServer(getKeyManagers());

    ClientChannelConfiguration channelConfig = ChannelConfiguration.clientConfig(1, "worker");
    ClientConfig clientConfig = new ClientConfig(ConfigFactory.load().getConfig("xio.baseClient"));
    ClientState clientState = new ClientState(channelConfig, clientConfig);
    testHandler = new TestHandler();
    client = new Client(clientState, () -> testHandler, null);
  }

  @After
  public void afterEach() throws Exception {
    mockWebServer.close();
  }

  @Test
  public void testWriteH1ServerPreservesStreamId() throws Exception {
    testWriteServerPreservesStreamId(false);
  }

  @Test
  public void testWriteH2ServerPreservesStreamId() throws Exception {
    testWriteServerPreservesStreamId(true);
  }

  public void testWriteServerPreservesStreamId(boolean h2Server) throws Exception {
    // given an h1 / h2 server supporting
    mockWebServer.setProtocols(
        h2Server
            ? Arrays.asList(Protocol.HTTP_1_1, Protocol.HTTP_2)
            : Collections.singletonList(Protocol.HTTP_1_1));
    mockWebServer.enqueue(new MockResponse());
    mockWebServer.start();

    // when we proxy an h2 request with a streamId
    int expectedId = 3;
    Request request =
        DefaultFullRequest.builder()
            .method(GET)
            .path("/v1/canonical/cats/meow")
            .host("127.0.0.1")
            .body(Unpooled.EMPTY_BUFFER)
            .streamId(expectedId)
            .build();

    latch = new CountDownLatch(1);
    client
        .connect(new InetSocketAddress(request.host(), mockWebServer.getPort()))
        .addListener((ignored) -> client.write(request));
    latch.await(10000, TimeUnit.SECONDS);

    // then the original h2 stream id is preserved
    assertNotNull("expected a response", testHandler.response);
    assertEquals(
        "expected the stream id to be preserved", expectedId, testHandler.response.streamId());
  }

  private class TestHandler extends SimpleChannelInboundHandler<Response> {

    Throwable error;
    Response response;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {
      latch.countDown();
      this.response = response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable error) throws Exception {
      latch.countDown();
      this.error = error;
    }
  }
}
