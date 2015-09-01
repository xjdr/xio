package com.xjeffrose.xio.server;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.xjeffrose.xio.client.HttpClientChannel;
import com.xjeffrose.xio.client.HttpClientConnector;
import com.xjeffrose.xio.client.XioClient;
import com.xjeffrose.xio.client.XioClientChannel;
import com.xjeffrose.xio.client.XioClientConfig;
import com.xjeffrose.xio.client.XioException;
import com.xjeffrose.xio.core.XioCodecFactory;
import com.xjeffrose.xio.core.XioNoOpHandler;
import com.xjeffrose.xio.core.XioNoOpSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityFactory;
import com.xjeffrose.xio.core.XioSecurityHandlers;
import com.xjeffrose.xio.core.XioTimer;
import com.xjeffrose.xio.fixtures.SimpleTestServer;
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
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.junit.Test;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class XioServerTransportTest {

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
        .withProcessorFactory(new XioProcessorFactory() {
          @Override
          public XioProcessor getProcessor() {
            return new XioProcessor() {

              @Override
              public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, Object _request, RequestContext respCtx) {

                ListeningExecutorService service = MoreExecutors.listeningDecorator(ctx.executor());
                ListenableFuture<Boolean> httpResponseFuture = service.submit(new Callable<Boolean>() {
                  public Boolean call() {
                    HttpRequest request = null;

                    if (_request instanceof HttpRequest) {
                      request = (HttpRequest) _request;
                    }

                    final StringBuilder buf = new StringBuilder();

                    buf.setLength(0);
                    buf.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
                    buf.append("===================================\r\n");

                    buf.append("VERSION: ").append(request.getProtocolVersion()).append("\r\n");
                    buf.append("HOSTNAME: ").append(HttpHeaders.getHost(request, "unknown")).append("\r\n");
                    buf.append("REQUEST_URI: ").append(request.getUri()).append("\r\n\r\n");

                    HttpHeaders headers = request.headers();
                    if (!headers.isEmpty()) {
                      for (Map.Entry<String, String> h : headers) {
                        String key = h.getKey();
                        String value = h.getValue();
                        buf.append("HEADER: ").append(key).append(" = ").append(value).append("\r\n");
                      }
                      buf.append("\r\n");
                    }

                    boolean keepAlive = HttpHeaders.isKeepAlive(request);
                    // Build the response object.
                    FullHttpResponse response = new DefaultFullHttpResponse(
                        HTTP_1_1, request.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
                        Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

                    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

                    if (keepAlive) {
                      // Add keep alive header as per:
                      // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
                      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                    }

                    respCtx.setContextData(respCtx.getConnectionId(), response);

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
        .listen(new InetSocketAddress(8082))
//        .listen(new InetSocketAddress("127.0.0.1", 8082))
        .name("Xio Test Server")
        .taskTimeout(new Duration((double) 20000, TimeUnit.MILLISECONDS))
        .using(Executors.newCachedThreadPool())
        .withSecurityFactory(new XioSecurityFactory() {
          @Override
          public XioSecurityHandlers getSecurityHandlers(XioServerDef def, XioServerConfig serverConfig) {
            return new XioSecurityHandlers() {
              @Override
              public ChannelHandler getAuthenticationHandler() {
                return new XioNoOpHandler();
              }

              @Override
              public ChannelHandler getEncryptionHandler() {
                try {
                  SelfSignedCertificate ssc = new SelfSignedCertificate();
                  SslContext sslCtx = SslContext.newServerContext(SslContext.defaultServerProvider(), ssc.certificate(), ssc.privateKey());

//                SSLEngine engine = new SSLEngineFactory("src/test/resources/privateKey.pem", "src/test/resources/cert.pem").getEngine();
//                engine.beginHandshake();

                  return sslCtx.newHandler(new PooledByteBufAllocator());

                } catch (SSLException | CertificateException e) {
                  e.printStackTrace();
                }
//                return new SslHandler(engine);
                return null;
              }
            };
          }

          @Override
          public XioSecurityHandlers getSecurityHandlers(XioClientConfig clientConfig) {
            return null;
          }
        })
        .withProcessorFactory(new XioProcessorFactory() {
          @Override
          public XioProcessor getProcessor() {
            return new XioProcessor() {

              @Override
              public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, Object _request, RequestContext respCtx) {
                ListeningExecutorService service = MoreExecutors.listeningDecorator(ctx.executor());
                ListenableFuture<Boolean> httpResponseFuture = service.submit(new Callable<Boolean>() {
                  public Boolean call() {
                    HttpRequest request = null;

                    if (_request instanceof HttpRequest) {
                      request = (HttpRequest) _request;
                    }

                    final StringBuilder buf = new StringBuilder();

                    buf.setLength(0);
                    buf.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
                    buf.append("===================================\r\n");

                    buf.append("VERSION: ").append(request.getProtocolVersion()).append("\r\n");
                    buf.append("HOSTNAME: ").append(HttpHeaders.getHost(request, "unknown")).append("\r\n");
                    buf.append("REQUEST_URI: ").append(request.getUri()).append("\r\n\r\n");

                    HttpHeaders headers = request.headers();
                    if (!headers.isEmpty()) {
                      for (Map.Entry<String, String> h : headers) {
                        String key = h.getKey();
                        String value = h.getValue();
                        buf.append("HEADER: ").append(key).append(" = ").append(value).append("\r\n");
                      }
                      buf.append("\r\n");
                    }

                    boolean keepAlive = HttpHeaders.isKeepAlive(request);
                    // Build the response object.
                    FullHttpResponse response = new DefaultFullHttpResponse(
                        HTTP_1_1, request.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
                        Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

                    response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

                    if (keepAlive) {
                      // Add keep alive header as per:
                      // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
                      response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                    }

                    respCtx.setContextData(respCtx.getConnectionId(), response);

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

    // For Integration Testing (LEAVE OUT!!!!)
    Thread.sleep(20000000);

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
  public void testProxy() throws Exception {
    SimpleTestServer testServer = new SimpleTestServer(8085);
    testServer.run();

    XioServerDef serverDef = new XioServerDefBuilder()
        .clientIdleTimeout(new Duration((double) 200, TimeUnit.MILLISECONDS))
        .limitConnectionsTo(200)
        .limitFrameSizeTo(1024)
        .limitQueuedResponsesPerConnection(50)
        .listen(new InetSocketAddress(8084))
//        .listen(new InetSocketAddress("127.0.0.1", 8082))
        .name("Xio Test Server")
        .taskTimeout(new Duration((double) 20000, TimeUnit.MILLISECONDS))
        .using(Executors.newCachedThreadPool())
        .withSecurityFactory(new XioSecurityFactory() {
          @Override
          public XioSecurityHandlers getSecurityHandlers(XioServerDef def, XioServerConfig serverConfig) {
            return new XioSecurityHandlers() {
              @Override
              public ChannelHandler getAuthenticationHandler() {
                return new XioNoOpHandler();
              }

              @Override
              public ChannelHandler getEncryptionHandler() {
                try {
                  SelfSignedCertificate ssc = new SelfSignedCertificate();
                  SslContext sslCtx = SslContext.newServerContext(SslContext.defaultServerProvider(), ssc.certificate(), ssc.privateKey());

//                SSLEngine engine = new SSLEngineFactory("src/test/resources/privateKey.pem", "src/test/resources/cert.pem").getEngine();
//                engine.beginHandshake();

                  return sslCtx.newHandler(new PooledByteBufAllocator());

                } catch (SSLException | CertificateException e) {
                  e.printStackTrace();
                }
//                return new SslHandler(engine);
                return null;
              }
            };
          }

          @Override
          public XioSecurityHandlers getSecurityHandlers(XioClientConfig clientConfig) {
            return null;
          }
        })
        .withProcessorFactory(new XioProcessorFactory() {
          @Override
          public XioProcessor getProcessor() {
            return new XioProcessor() {
              @Override
              public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, Object _request, RequestContext respCtx) {
                ListeningExecutorService service = MoreExecutors.listeningDecorator(ctx.executor());
                HttpRequest request = null;

                if (_request instanceof HttpRequest) {
                  request = (HttpRequest) _request;
                }

                try {
                  XioClient xioClient = new XioClient();
                  ListenableFuture<XioClientChannel> responseFuture = null;

                  responseFuture = xioClient.connectAsync(new HttpClientConnector(new URI("http://localhost:8085")));

                  XioClientChannel xioClientChannel = responseFuture.get();
                  HttpClientChannel httpClientChannel = (HttpClientChannel) xioClientChannel;

                  ByteBuf resp = httpClientChannel.sendProxyRequest(Unpooled.EMPTY_BUFFER);
                  respCtx.setContextData(respCtx.getConnectionId(), resp);

                } catch (URISyntaxException | InterruptedException | XioException | ExecutionException e) {
                  e.printStackTrace();
                }

                ListenableFuture<Boolean> httpResponseFuture = service.submit(new Callable<Boolean>() {
                  public Boolean call() {
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
            return new HttpRequestDecoder();
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

    // For Integration Testing (LEAVE OUT!!!!)
    Thread.sleep(20000000);

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
