package com.xjeffrose.xio.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.xjeffrose.xio.EchoClient;
import com.xjeffrose.xio.EchoServer;
import com.xjeffrose.xio.client.HttpClientChannel;
import com.xjeffrose.xio.client.HttpClientConnector;
import com.xjeffrose.xio.client.Listener;
import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioClientChannel;
import com.xjeffrose.xio.client.XioClientConfig;
import com.xjeffrose.xio.client.retry.BoundedExponentialBackoffRetry;
import com.xjeffrose.xio.core.BBtoHttpResponse;
import com.xjeffrose.xio.core.TcpCodec;
import com.xjeffrose.xio.core.XioAggregatorFactory;
import com.xjeffrose.xio.core.XioCodecFactory;
import com.xjeffrose.xio.core.XioException;
import com.xjeffrose.xio.core.XioNoOpHandler;
import com.xjeffrose.xio.core.XioNoOpSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import com.xjeffrose.xio.core.XioTimer;
import com.xjeffrose.xio.core.XioTransportException;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import com.xjeffrose.xio.fixtures.SimpleTestServer;
import com.xjeffrose.xio.fixtures.TcpClient;
import com.xjeffrose.xio.fixtures.XioTestProcessorFactory;
import com.xjeffrose.xio.fixtures.XioTestSecurityFactory;
import com.xjeffrose.xio.processor.XioProcessor;
import com.xjeffrose.xio.processor.XioProcessorFactory;
import io.airlift.units.Duration;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLException;
import org.apache.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class XioServerFunctionalTest {
  private static final Logger log = Logger.getLogger(XioServerFunctionalTest.class.getName());

  public static final XioTimer timer = new XioTimer("Test Timer", (long) 100, TimeUnit.MILLISECONDS, 100);

  @Test
  public void testComplexServerConfigurationTCP() throws Exception {
    EchoServer tcpServer = new EchoServer(9002);
    new Thread(tcpServer).start();

    XioServerDef serverDef = new XioServerDefBuilder()
        .clientIdleTimeout(new Duration((double) 2000, TimeUnit.MILLISECONDS))
        .limitConnectionsTo(20)
        .limitFrameSizeTo(1024)
        .limitQueuedResponsesPerConnection(5)
        .listen(new InetSocketAddress(9001))
        .name("Xio Tcp Test Server")
        .taskTimeout(new Duration((double) 2000, TimeUnit.MILLISECONDS))
        .using(Executors.newCachedThreadPool())
        .withSecurityFactory(new XioNoOpSecurityFactory())
        .withCodecFactory(() -> new TcpCodec())
        .withAggregator(() -> new XioNoOpHandler())
        .withRoutingFilter(() -> new XioNoOpHandler())
        .withProcessorFactory(
            new XioProcessorFactory() {
              @Override
              public XioProcessor getProcessor() {
                return new XioProcessor() {

                  @Override
                  public void disconnect(ChannelHandlerContext ctx) {

                  }

                  @Override
                  public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, Object request, RequestContext reqCtx) {
                    ListeningExecutorService service = MoreExecutors.listeningDecorator(ctx.executor());

                    return service.submit(() -> {
                      reqCtx.setContextData(reqCtx.getConnectionId(), request);
                      return true;
                    });
                  }
                };
              }
            }
        )
        .build();

    XioServerConfig serverConfig = new XioServerConfigBuilder()
        .setBossThreadCount(2)
        .setBossThreadExecutor(Executors.newCachedThreadPool())
        .setWorkerThreadCount(2)
        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
        .setTimer(timer)
        .setXioName("Xio Name Test")
        .build();

    DefaultChannelGroup defaultChannelGroup;

    if (System.getProperty("os.name") == "Linux") {
      defaultChannelGroup =  new DefaultChannelGroup(new EpollEventLoopGroup().next());
    } else {
      defaultChannelGroup = new DefaultChannelGroup(new NioEventLoopGroup().next());
    }

      // Create the server transport
    final XioServerTransport server = new XioServerTransport(serverDef,
        serverConfig, defaultChannelGroup);

    // Start the server
    server.start();

    // Use 3rd party client to test proper operation
    //TODO(JR): Figure out why \n seems to get chopped off
    String expectedResponse = "Working TcpServer";

    EchoClient echoClient = new EchoClient("127.0.0.1", 9002);
    echoClient.start();

//    String response = TcpClient.sendReq("127.0.0.1", 9001, expectedResponse);
//    assertEquals(expectedResponse, response);

    // Arrange to stop the server at shutdown
    Runtime.getRuntime().

        addShutdownHook(new Thread() {
                          @Override
                          public void run() {
                            try {
                              server.stop();
                            } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                            }
                          }
                        }

        );
  }

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
        .withRoutingFilter(() -> new XioNoOpHandler())
        .withProcessorFactory(new XioTestProcessorFactory())
        .withCodecFactory(new XioCodecFactory() {
          @Override
          public ChannelHandler getCodec() {
            return new HttpServerCodec();
          }
        })
        .withAggregator(new XioAggregatorFactory() {
          @Override
          public ChannelHandler getAggregator() {
            return new HttpObjectAggregator(16777216);
          }
        })
        .build();

    XioServerConfig serverConfig = new XioServerConfigBuilder()
        .setBossThreadCount(2)
        .setBossThreadExecutor(Executors.newCachedThreadPool())
        .setWorkerThreadCount(2)
        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
        .setTimer(timer)
        .setXioName("Xio Name Test")
        .build();


    DefaultChannelGroup defaultChannelGroup;

    if (System.getProperty("os.name") == "Linux") {
      defaultChannelGroup =  new DefaultChannelGroup(new EpollEventLoopGroup().next());
    } else {
      defaultChannelGroup = new DefaultChannelGroup(new NioEventLoopGroup().next());
    }

    // Create the server transport
    final XioServerTransport server = new XioServerTransport(serverDef,
        serverConfig, defaultChannelGroup);

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
        "HEADER: User-Agent = okhttp/2.4.0\r\n" +
        "HEADER: Content-Length = 0\r\n\r\n";

    assertTrue(response.isSuccessful());
    assertEquals(200, response.code());
    assertEquals(expectedResponse, response.body().string());

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
        .withRoutingFilter(() -> new XioNoOpHandler())
        .withCodecFactory(new XioCodecFactory() {
          @Override
          public ChannelHandler getCodec() {
            return new HttpServerCodec();
          }
        })
        .withAggregator(new XioAggregatorFactory() {
          @Override
          public ChannelHandler getAggregator() {
            return new HttpObjectAggregator(16777216);
          }
        }).build();

    XioServerConfig serverConfig = new XioServerConfigBuilder()
        .setBossThreadCount(2)
        .setBossThreadExecutor(Executors.newCachedThreadPool())
        .setWorkerThreadCount(2)
        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
        .setTimer(timer)
        .setXioName("Xio Name Test")
        .build();


    DefaultChannelGroup defaultChannelGroup;

    if (System.getProperty("os.name") == "Linux") {
      defaultChannelGroup =  new DefaultChannelGroup(new EpollEventLoopGroup().next());
    } else {
      defaultChannelGroup = new DefaultChannelGroup(new NioEventLoopGroup().next());
    }

    // Create the server transport
    final XioServerTransport server = new XioServerTransport(serverDef,
        serverConfig, defaultChannelGroup);

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
        "HEADER: User-Agent = okhttp/2.4.0\r\n" +
        "HEADER: Content-Length = 0\r\n\r\n";

    assertEquals(200, response.code());
    assertEquals(expectedResponse, response.body().string());

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
  public void testSimpleProxy() throws Exception {
    SimpleTestServer testServer = new SimpleTestServer(8089);
    testServer.run();

    XioServerDef serverDef = new XioServerDefBuilder()
        .clientIdleTimeout(new Duration((double) 200, TimeUnit.MILLISECONDS))
        .limitConnectionsTo(200)
        .limitFrameSizeTo(1024)
        .limitQueuedResponsesPerConnection(50)
        .clientIdleTimeout(new Duration(60, TimeUnit.SECONDS))
        .listen(new InetSocketAddress(8088))
//        .listen(new InetSocketAddress("127.0.0.1", 8082))
        .name("Xio Test Server")
        .taskTimeout(new Duration((double) 20000, TimeUnit.MILLISECONDS))
        .using(Executors.newCachedThreadPool())
        .withSecurityFactory(new XioTestSecurityFactory())
        .withRoutingFilter(() -> new XioNoOpHandler())
        .withProcessorFactory(new XioProcessorFactory() {
          @Override
          public XioProcessor getProcessor() {

            return new XioProcessor() {

              @Override
              public void disconnect(ChannelHandlerContext ctx) {

              }

              @Override
              public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, Object request, RequestContext reqCtx) {
                final ListeningExecutorService service = MoreExecutors.listeningDecorator(ctx.executor());

                ListenableFuture<Boolean> httpResponseFuture = service.submit(new Callable<Boolean>() {
                  @Override
                  public Boolean call() throws Exception {
                    final Lock lock = new ReentrantLock();
                    final Condition waitForFinish = lock.newCondition();
                    final XioClient xioClient = new XioClient();

                    ListenableFuture<XioClientChannel> responseFuture = null;

                    responseFuture = xioClient.connectAsync(ctx, new HttpClientConnector(new URI("http://localhost:8089")), new BoundedExponentialBackoffRetry(100, 10000, 3));

                    XioClientChannel xioClientChannel = null;

                    if (!responseFuture.isCancelled()) {
                      xioClientChannel = responseFuture.get((long) 2000, TimeUnit.MILLISECONDS);
                    }

                    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

                    Map<String, String> headerMap = ImmutableMap.of(
                        HttpHeaders.HOST, "localhost:8089",
                        HttpHeaders.USER_AGENT, "xio/0.7.8",
                        HttpHeaders.CONTENT_TYPE, "application/text",
                        HttpHeaders.ACCEPT_ENCODING, "*/*"
                    );

                    httpClientChannel.setHeaders(headerMap);

                    Listener<ByteBuf> listener = new Listener<ByteBuf>() {
                      ByteBuf response = null;

                      @Override
                      public void onRequestSent() {
//                        System.out.println("Request Sent");
                      }

                      @Override
                      public void onResponseReceived(ByteBuf message) {
                        response = message;

                      }

                      @Override
                      public void onChannelError(XioException requestException) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(HttpVersion.HTTP_1_1)
                            .append(" ")
                            .append(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                            .append("\r\n")
                            .append("\r\n\r\n")
                            .append(requestException.getMessage())
                            .append("\n");

                        response = Unpooled.wrappedBuffer(sb.toString().getBytes());

                      }

                      @Override
                      public ByteBuf getResponse() {
                        if (response != null) {
                          return response;
                        } else {
                          try {
                            Thread.sleep(200);
                          } catch (InterruptedException e) {
                            e.printStackTrace();
                          }
                          return getResponse();
                        }
                      }

                    };

                    httpClientChannel.sendAsynchronousRequest(Unpooled.EMPTY_BUFFER, false, listener);


                    DefaultFullHttpResponse httpResponse = BBtoHttpResponse.getResponse(listener.getResponse());

                    assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
                    assertEquals("Jetty(9.3.1.v20150714)", httpResponse.headers().get("Server"));
                    assertEquals("CONGRATS!\n\r\n", httpResponse.content().toString(Charset.defaultCharset()));

                    reqCtx.setContextData(reqCtx.getConnectionId(), httpResponse);
                    return true;
                  }

                });
                return httpResponseFuture;
              }
            };
          }
        })
        .withCodecFactory(new XioCodecFactory() {
          @Override
          public ChannelHandler getCodec() {
            return new HttpServerCodec();
          }
        })
        .withAggregator(new XioAggregatorFactory() {
          @Override
          public ChannelHandler getAggregator() {
            return new HttpObjectAggregator(16777216);
          }
        })
        .build();

    XioServerConfig serverConfig = new XioServerConfigBuilder()
        .setBossThreadCount(2)
        .setBossThreadExecutor(Executors.newCachedThreadPool())
        .setWorkerThreadCount(2)
        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
        .setTimer(timer)
        .setXioName("Xio Name Test")
        .build();


    DefaultChannelGroup defaultChannelGroup;

    if (System.getProperty("os.name") == "Linux") {
      defaultChannelGroup =  new DefaultChannelGroup(new EpollEventLoopGroup().next());
    } else {
      defaultChannelGroup = new DefaultChannelGroup(new NioEventLoopGroup().next());
    }

    // Create the server transport
    final XioServerTransport server = new XioServerTransport(serverDef,
        serverConfig, defaultChannelGroup);

    // Start the server
    server.start();

    // Use 3rd party client to test proper operation
    Request request = new Request.Builder()
        .url("https://127.0.0.1:8088/")
        .build();

    Response response = OkHttpUnsafe.getUnsafeClient().newCall(request).execute();
    assertEquals(200, response.code());
    assertEquals("CONGRATS!\n", response.body().string());

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
  public void testComplexProxy() throws Exception {

    XioServerDef serverDef = new XioServerDefBuilder()
        .clientIdleTimeout(new Duration((double) 200, TimeUnit.MILLISECONDS))
        .limitConnectionsTo(200)
        .limitFrameSizeTo(1024)
        .limitQueuedResponsesPerConnection(50)
        .listen(new InetSocketAddress(8090))
//        .listen(new InetSocketAddress("127.0.0.1", 8082))
        .name("Xio Test Server")
        .taskTimeout(new Duration((double) 20000, TimeUnit.MILLISECONDS))
        .using(Executors.newCachedThreadPool())
        .withSecurityFactory(new XioTestSecurityFactory())
        .withRoutingFilter(() -> new XioNoOpHandler())
        .withProcessorFactory(new XioProcessorFactory() {

          @Override
          public XioProcessor getProcessor() {
            return new XioProcessor() {

              @Override
              public void disconnect(ChannelHandlerContext ctx) {

              }

              @Override
              public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, Object request, RequestContext reqCtx) {
                final ListeningExecutorService service = MoreExecutors.listeningDecorator(ctx.executor());

                ListenableFuture<Boolean> httpResponseFuture = service.submit(new Callable<Boolean>() {
                  @Override
                  public Boolean call() throws Exception {
                    final Lock lock = new ReentrantLock();
                    final Condition waitForFinish = lock.newCondition();
                    final XioClientConfig xioClientConfig = XioClientConfig.newBuilder()
                        .setSecurityFactory(new XioSecurityFactory() {
                          @Override
                          public XioSecurityHandlers getSecurityHandlers(XioServerDef def, XioServerConfig serverConfig) {
                            return null;
                          }

                          @Override
                          public XioSecurityHandlers getSecurityHandlers() {
                            return new XioSecurityHandlers() {
                              @Override
                              public ChannelHandler getAuthenticationHandler() {
                                return new XioNoOpHandler();
                              }

                              @Override
                              public ChannelHandler getEncryptionHandler() {
                                try {
                                  SslContext sslCtx = SslContextBuilder
                                      .forClient()
                                      .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                      .build();

                                  return sslCtx.newHandler(new PooledByteBufAllocator());
                                } catch (SSLException e) {
                                  e.printStackTrace();
                                }
                                return null;
                              }
                            };
                          }
                        })
                        .build();
                    final XioClient xioClient = new XioClient(xioClientConfig);
                    final ListenableFuture<XioClientChannel> responseFuture = xioClient.connectAsync(ctx, new HttpClientConnector(new URI("https://www.paypal.com/home")), new BoundedExponentialBackoffRetry(1000, 100000, 3));

                    XioClientChannel xioClientChannel;

                    if (!responseFuture.isCancelled()) {
                      xioClientChannel = responseFuture.get((long) 2000, TimeUnit.MILLISECONDS);
                    } else {
                      throw new XioTransportException("Client Timeout");
                    }

                    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

                    Map<String, String> headerMap = ImmutableMap.of(
                        HttpHeaders.HOST, "www.paypal.com",
                        HttpHeaders.USER_AGENT, "xio/0.7.8",
                        HttpHeaders.CONTENT_TYPE, "application/text",
                        HttpHeaders.ACCEPT_ENCODING, "*/*"
                    );

                    httpClientChannel.setHeaders(headerMap);

                    Listener<ByteBuf> listener = new Listener<ByteBuf>() {
                      ByteBuf response = null;

                      @Override
                      public void onRequestSent() {
//                        System.out.println("Request Sent");
                      }

                      @Override
                      public void onResponseReceived(ByteBuf message) {
                        response = message;
                      }

                      @Override
                      public void onChannelError(XioException requestException) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(HttpVersion.HTTP_1_1)
                            .append(" ")
                            .append(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                            .append("\r\n")
                            .append("\r\n\r\n")
                            .append(requestException.getMessage())
                            .append("\n");

                        response = Unpooled.wrappedBuffer(sb.toString().getBytes());

                      }

                      @Override
                      public ByteBuf getResponse() {
                        if (response != null) {
                          return response;
                        } else {
                          try {
                            Thread.sleep(200);
                          } catch (InterruptedException e) {
                            e.printStackTrace();
                          }
                          return getResponse();
                        }
                      }

                    };

                    httpClientChannel.sendAsynchronousRequest(Unpooled.EMPTY_BUFFER, false, listener);

                    DefaultFullHttpResponse httpResponse = BBtoHttpResponse.getResponse(listener.getResponse());

                    assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
//                    assertEquals("nginx/1.6.0", httpResponse.headers().get("Server"));
                    assertTrue(httpResponse.content() != null);

                    reqCtx.setContextData(reqCtx.getConnectionId(), httpResponse);
                    return true;
                  }

                });
                return httpResponseFuture;
              }
            };
          }
        })
        .withCodecFactory(new XioCodecFactory() {
          @Override
          public ChannelHandler getCodec() {
            return new HttpServerCodec();
          }
        })
        .withAggregator(new XioAggregatorFactory() {
          @Override
          public ChannelHandler getAggregator() {
            return new HttpObjectAggregator(16777216);
          }
        })
        .build();

    XioServerConfig serverConfig = new XioServerConfigBuilder()
        .setBossThreadCount(2)
        .setBossThreadExecutor(Executors.newCachedThreadPool())
        .setWorkerThreadCount(2)
        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
        .setTimer(timer)
        .setXioName("Xio Name Test")
        .build();


    DefaultChannelGroup defaultChannelGroup;

    if (System.getProperty("os.name") == "Linux") {
      defaultChannelGroup =  new DefaultChannelGroup(new EpollEventLoopGroup().next());
    } else {
      defaultChannelGroup = new DefaultChannelGroup(new NioEventLoopGroup().next());
    }

    // Create the server transport
    final XioServerTransport server = new XioServerTransport(serverDef,
        serverConfig, defaultChannelGroup);

    // Start the server
    server.start();

    // Use 3rd party client to test proper operation
    Request request = new Request.Builder()
        .url("https://127.0.0.1:8090/")
        .build();

    Response response = OkHttpUnsafe.getUnsafeClient().newCall(request).execute();
    assertEquals(200, response.code());
    assertTrue(!response.body().string().isEmpty());

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

}
