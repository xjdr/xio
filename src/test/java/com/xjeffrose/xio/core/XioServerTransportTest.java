package com.xjeffrose.xio.core;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.xjeffrose.xio.processor.XioProcessor;
import com.xjeffrose.xio.processor.XioProcessorFactory;
import com.xjeffrose.xio.server.RequestContext;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerConfigBuilder;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerDefBuilder;
import com.xjeffrose.xio.server.XioServerTransport;
import io.airlift.units.Duration;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpServerCodec;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

public class XioServerTransportTest {

  @Test
  public void testComplexServerConfiguration() throws Exception {
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
              public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, HttpRequest req, RequestContext respCtx) {
                ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
                ListenableFuture<Boolean> httpResponseFuture = service.submit(new Callable<Boolean>() {
                  public Boolean call() {

                    respCtx.setContextData(respCtx.getConnectionId(), new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));

                    return true;
                  }
                });
                return httpResponseFuture;
              }

              @Override
              public void executeInIoThread(Runnable runnable) {

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
        new DefaultChannelGroup());

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
