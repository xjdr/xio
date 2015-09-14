package com.xjeffrose.xio.server;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.xjeffrose.xio.core.XioCodecFactory;
import com.xjeffrose.xio.core.XioNoOpSecurityFactory;
import com.xjeffrose.xio.core.XioTimer;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import com.xjeffrose.xio.fixtures.XioTestProcessorFactory;
import com.xjeffrose.xio.fixtures.XioTestSecurityFactory;
import io.airlift.units.Duration;
import io.netty.channel.ChannelHandler;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.HttpServerCodec;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class XioServerFunctionalTest {

  @Test
  public void testComplexServerConfigurationHttp() throws Exception {
    XioServerDef serverDef = new XioServerDefBuilder()
        .clientIdleTimeout(new Duration((double) 200, TimeUnit.MILLISECONDS))
        .limitConnectionsTo(200)
        .limitFrameSizeTo(1024)
        .limitQueuedResponsesPerConnection(50)
        .listen(new InetSocketAddress(8083))
//        .listen(new InetSocketAddress("127.0.0.1", 8082))
        .name("Xio Test Server")
        .taskTimeout(new Duration((double) 20000, TimeUnit.MILLISECONDS))
        .using(Executors.newCachedThreadPool())
        .withSecurityFactory(new XioNoOpSecurityFactory())
        .withProcessorFactory(new XioTestProcessorFactory())
        .withCodecFactory(new XioCodecFactory() {
          @Override
          public ChannelHandler getCodec() {
            return new HttpServerCodec();
          }
        })
        .build();

    XioServerConfig serverConfig = new XioServerConfigBuilder()
        .setBossThreadCount(12)
        .setBossThreadExecutor(Executors.newCachedThreadPool())
        .setWorkerThreadCount(20)
        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
        .setTimer(new XioTimer("Test Timer", (long) 100, TimeUnit.MILLISECONDS, 100))
        .setXioName("Xio Name Test")
        .build();

    // Create the server transport
    final XioServerTransport server = new XioServerTransport(serverDef,
        serverConfig,
        new DefaultChannelGroup(new NioEventLoopGroup().next()));

    // Start the server
    server.start();

    // Use 3rd party client to test proper operation
    Request request = new Request.Builder()
        .url("http://127.0.0.1:8083/")
        .build();

    OkHttpClient client = new OkHttpClient();
    Response response = client.newCall(request).execute();

    String expectedResponse = "WELCOME TO THE WILD WILD WEB SERVER\r\n" +
        "===================================\r\n" +
        "VERSION: HTTP/1.1\r\n" +
        "HOSTNAME: 127.0.0.1:8083\r\n" +
        "REQUEST_URI: /\r\n" +
        "\r\n" +
        "HEADER: Host = 127.0.0.1:8083\r\n" +
        "HEADER: Connection = Keep-Alive\r\n" +
        "HEADER: Accept-Encoding = gzip\r\n" +
        "HEADER: User-Agent = okhttp/2.4.0\r\n\r\n";

    assertTrue(response.isSuccessful());
    assertEquals(200, response.code());
    assertEquals(expectedResponse, response.body().string());

    // For Integration Testing (LEAVE OUT!!!!)
//    Thread.sleep(20000000);

    // Arrange to stop the server at shutdown
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          server.stop();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
  }

  @Test
  public void testComplexServerConfigurationHttps() throws Exception {
    XioServerDef serverDef = new XioServerDefBuilder()
        .clientIdleTimeout(new Duration((double) 200, TimeUnit.MILLISECONDS))
        .limitConnectionsTo(200)
        .limitFrameSizeTo(1024)
        .limitQueuedResponsesPerConnection(50)
        .listen(new InetSocketAddress(8087))
//        .listen(new InetSocketAddress("127.0.0.1", 8082))
        .name("Xio Test Server")
        .taskTimeout(new Duration((double) 20000, TimeUnit.MILLISECONDS))
        .using(Executors.newCachedThreadPool())
        .withSecurityFactory(new XioTestSecurityFactory())
        .withProcessorFactory(new XioTestProcessorFactory())
        .withCodecFactory(new XioCodecFactory() {
          @Override
          public ChannelHandler getCodec() {
            return new HttpServerCodec();
          }
        })
        .build();

    XioServerConfig serverConfig = new XioServerConfigBuilder()
        .setBossThreadCount(12)
        .setBossThreadExecutor(Executors.newCachedThreadPool())
        .setWorkerThreadCount(20)
        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
        .setTimer(new XioTimer("Test Timer", (long) 100, TimeUnit.MILLISECONDS, 100))
        .setXioName("Xio Name Test")
        .build();

    // Create the server transport
    final XioServerTransport server = new XioServerTransport(serverDef,
        serverConfig,
        new DefaultChannelGroup(new NioEventLoopGroup().next()));

    // Start the server
    server.start();

    // Use 3rd party client to test proper operation
    Request request = new Request.Builder()
        .url("https://127.0.0.1:8087/")
        .build();

    Response response = OkHttpUnsafe.getUnsafeClient().newCall(request).execute();

    String expectedResponse = "WELCOME TO THE WILD WILD WEB SERVER\r\n" +
        "===================================\r\n" +
        "VERSION: HTTP/1.1\r\n" +
        "HOSTNAME: 127.0.0.1:8087\r\n" +
        "REQUEST_URI: /\r\n" +
        "\r\n" +
        "HEADER: Host = 127.0.0.1:8087\r\n" +
        "HEADER: Connection = Keep-Alive\r\n" +
        "HEADER: Accept-Encoding = gzip\r\n" +
        "HEADER: User-Agent = okhttp/2.4.0\r\n\r\n";

    assertEquals(200, response.code());
    assertEquals(expectedResponse, response.body().string());

    // For Integration Testing (LEAVE OUT!!!!)
//    Thread.sleep(20000000);

    // Arrange to stop the server at shutdown
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          server.stop();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
  }


//  @Test
//  public void testProxy() throws Exception {
//    SimpleTestServer testServer = new SimpleTestServer(8085);
//    testServer.run();
//
//    XioServerDef serverDef = new XioServerDefBuilder()
//        .clientIdleTimeout(new Duration((double) 200, TimeUnit.MILLISECONDS))
//        .limitConnectionsTo(200)
//        .limitFrameSizeTo(1024)
//        .limitQueuedResponsesPerConnection(50)
//        .listen(new InetSocketAddress(8084))
////        .listen(new InetSocketAddress("127.0.0.1", 8082))
//        .name("Xio Test Server")
//        .taskTimeout(new Duration((double) 20000, TimeUnit.MILLISECONDS))
//        .using(Executors.newCachedThreadPool())
//        .withSecurityFactory(new XioTestSecurityFactory())
//        .withProcessorFactory(new XioProcessorFactory() {
//          @Override
//          public XioProcessor getProcessor() {
//            return new XioProcessor() {
//              @Override
//              public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, Object _request, RequestContext respCtx) {
//                ListeningExecutorService service = MoreExecutors.listeningDecorator(ctx.executor());
//                HttpRequest request = null;
//
//                if (_request instanceof HttpRequest) {
//                  request = (HttpRequest) _request;
//                }
//
//                try {
//                  XioClient xioClient = new XioClient();
//                  ListenableFuture<XioClientChannel> responseFuture = null;
//
//                  responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("http://localhost:8085")));
//
//                  XioClientChannel xioClientChannel = responseFuture.get();
//                  HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;
//
//                  ByteBuf resp = httpClientChannel.sendProxyRequest(Unpooled.EMPTY_BUFFER);
//                  respCtx.setContextData(respCtx.getConnectionId(), resp);
//
//                } catch (URISyntaxException | InterruptedException | XioException | ExecutionException e) {
//                  e.printStackTrace();
//                }
//
//                ListenableFuture<Boolean> httpResponseFuture = service.submit(new Callable<Boolean>() {
//                  public Boolean call() {
//                    return true;
//                  }
//
//                });
//
//                return httpResponseFuture;
//              }
//            };
//          }
//        })
//        .withCodecFactory(new XioCodecFactory() {
//          @Override
//          public ChannelHandler getCodec() {
//            return new HttpRequestDecoder();
//          }
//        })
//        .build();
//
//    XioServerConfig serverConfig = new XioServerConfigBuilder()
//        .setBossThreadCount(12)
//        .setBossThreadExecutor(Executors.newCachedThreadPool())
//        .setWorkerThreadCount(20)
//        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
//        .setTimer(new XioTimer("Test Timer", (long) 100, TimeUnit.MILLISECONDS, 100))
//        .setXioName("Xio Name Test")
//        .build();
//
//    // Create the server transport
//    final XioServerTransport server = new XioServerTransport(serverDef,
//        serverConfig,
//        new DefaultChannelGroup(new NioEventLoopGroup().next()));
//
//    // Start the server
//    server.start();
//
//    // For Integration Testing (LEAVE OUT!!!!)
////    Thread.sleep(20000000);
//
//    // Arrange to stop the server at shutdown
//    Runtime.getRuntime().addShutdownHook(new Thread() {
//      @Override
//      public void run() {
//        try {
//          server.stop();
//        } catch (InterruptedException e) {
//          Thread.currentThread().interrupt();
//        }
//      }
//    });
//  }

}
