package com.xjeffrose.xio.server;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.xjeffrose.xio.client.HttpClientChannel;
import com.xjeffrose.xio.client.HttpClientConnector;
import com.xjeffrose.xio.client.Listener;
import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioClientChannel;
import com.xjeffrose.xio.client.XioClientConfig;
import com.xjeffrose.xio.core.XioCodecFactory;
import com.xjeffrose.xio.core.XioException;
import com.xjeffrose.xio.core.XioNoOpHandler;
import com.xjeffrose.xio.core.XioNoOpSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import com.xjeffrose.xio.core.XioTimer;
import com.xjeffrose.xio.fixtures.OkHttpUnsafe;
import com.xjeffrose.xio.fixtures.SimpleTestServer;
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
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLException;
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


  @Test
  public void testSimpleProxy() throws Exception {
    SimpleTestServer testServer = new SimpleTestServer(8089);
    testServer.run();

    XioServerDef serverDef = new XioServerDefBuilder()
        .clientIdleTimeout(new Duration((double) 200, TimeUnit.MILLISECONDS))
        .limitConnectionsTo(200)
        .limitFrameSizeTo(1024)
        .limitQueuedResponsesPerConnection(50)
        .listen(new InetSocketAddress(8088))
//        .listen(new InetSocketAddress("127.0.0.1", 8082))
        .name("Xio Test Server")
        .taskTimeout(new Duration((double) 20000, TimeUnit.MILLISECONDS))
        .using(Executors.newCachedThreadPool())
        .withSecurityFactory(new XioTestSecurityFactory())
        .withProcessorFactory(new XioProcessorFactory() {

          @Override
          public XioProcessor getProcessor() {
            return new XioProcessor() {
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

                    responseFuture = xioClient.connectAsync(ctx, new HttpClientConnector(new URI("http://localhost:8089")));

                    XioClientChannel xioClientChannel = null;

                    if (!responseFuture.isCancelled()) {
                      xioClientChannel = responseFuture.get((long) 2000, TimeUnit.MILLISECONDS);
                    }

                    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

                    Listener listener = new Listener() {
                      ByteBuf response;

                      @Override
                      public void onRequestSent() {
//                        System.out.println("Request Sent");
                      }

                      @Override
                      public void onResponseReceived(ByteBuf message) {
                        response = message;
                        lock.lock();
                        waitForFinish.signalAll();
                        lock.unlock();
                      }

                      @Override
                      public void onChannelError(XioException requestException) {
                        System.out.println("Request Error");
                        requestException.printStackTrace();
                      }

                      @Override
                      public ByteBuf getResponse() {
                        return response;
                      }

                    };

                    httpClientChannel.sendAsynchronousRequest(Unpooled.EMPTY_BUFFER, false, listener);

                    lock.lock();
                    waitForFinish.await();
                    lock.unlock();


                    // Lets make a HTTP parser cause apparently that's a good idea...
                    ByteBuf response = listener.getResponse();
                    String[] headerBody = response.toString(Charset.defaultCharset()).split("\r\n\r\n");
                    String[] headers = headerBody[0].split("\r\n");
                    String[] firstLine = headers[0].split("\\s");

                    // Lets make a HTTP Response object now
                    DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
                        HttpVersion.valueOf(firstLine[0]),
                        new HttpResponseStatus(Integer.parseInt(firstLine[1]), firstLine[2]),
                        httpClientChannel.getCtx().alloc().buffer().writeBytes(headerBody[1].getBytes()));

                    for (int i = 1; i < headers.length; i++) {
                      String[] xs = headers[i].split(":");
                      httpResponse.headers().add(xs[0].trim(), xs[1].trim());
                    }

                    assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
                    assertEquals("Jetty(9.3.1.v20150714)", httpResponse.headers().get("Server"));
                    assertEquals("CONGRATS!\n", httpResponse.content().toString(Charset.defaultCharset()));

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
        .url("https://127.0.0.1:8088/")
        .build();

    Response response = OkHttpUnsafe.getUnsafeClient().newCall(request).execute();
    assertEquals(200, response.code());
    assertEquals("CONGRATS!\n", response.body().string());

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
        .withProcessorFactory(new XioProcessorFactory() {

          @Override
          public XioProcessor getProcessor() {
            return new XioProcessor() {
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
//                SSLEngine engine = null;
                                try {
                                  SslContext sslCtx = SslContext.newClientContext(SslContext.defaultClientProvider(), InsecureTrustManagerFactory.INSTANCE);
//                  SSLEngine engine = new SSLEngineFactory(true).getEngine();
//                  engine.beginHandshake();
                                  return sslCtx.newHandler(new PooledByteBufAllocator());
                                } catch (SSLException e) {
                                  e.printStackTrace();
                                }
                                return null;
//                return new SslHandler(engine, false);
                              }
                            };
                          }
                        })
                        .build();
                    final XioClient xioClient = new XioClient(xioClientConfig);
                    ListenableFuture<XioClientChannel> responseFuture = null;

                    responseFuture = xioClient.connectAsync(ctx, new HttpClientConnector(new URI("https://www.paypal.com/home")));

                    XioClientChannel xioClientChannel = null;

                    if (!responseFuture.isCancelled()) {
                      xioClientChannel = responseFuture.get((long) 2000, TimeUnit.MILLISECONDS);
                    }

                    HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

                    Listener listener = new Listener() {
                      ByteBuf response;

                      @Override
                      public void onRequestSent() {
//                        System.out.println("Request Sent");
                      }

                      @Override
                      public void onResponseReceived(ByteBuf message) {
                        response = message;
                        lock.lock();
                        waitForFinish.signalAll();
                        lock.unlock();
                      }

                      @Override
                      public void onChannelError(XioException requestException) {
                        System.out.println("Request Error");
                        requestException.printStackTrace();
                      }

                      @Override
                      public ByteBuf getResponse() {
                        return response;
                      }

                    };

                    httpClientChannel.sendAsynchronousRequest(Unpooled.EMPTY_BUFFER, false, listener);

                    lock.lock();
                    waitForFinish.await();
                    lock.unlock();


                    // Lets make a HTTP parser cause apparently that's a good idea...
                    ByteBuf response = listener.getResponse();
                    String[] headerBody = response.toString(Charset.defaultCharset()).split("\r\n\r\n");
                    String[] headers = headerBody[0].split("\r\n");
                    String[] firstLine = headers[0].split("\\s");

                    // Lets make a HTTP Response object now
                    DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(
                        HttpVersion.valueOf(firstLine[0]),
                        new HttpResponseStatus(Integer.parseInt(firstLine[1]), firstLine[2]),
                        httpClientChannel.getCtx().alloc().buffer().writeBytes(headerBody[1].getBytes()));

                    for (int i = 1; i < headers.length; i++) {
                      String[] xs = headers[i].split(":");
                      httpResponse.headers().add(xs[0].trim(), xs[1].trim());
                    }

                    assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
                    assertEquals("nginx/1.6.0", httpResponse.headers().get("Server"));
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
        .url("https://127.0.0.1:8090/")
        .build();

    Response response = OkHttpUnsafe.getUnsafeClient().newCall(request).execute();
    assertEquals(200, response.code());
    assertTrue(!response.body().string().isEmpty());

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

}
