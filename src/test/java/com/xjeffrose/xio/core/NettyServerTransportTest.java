package com.xjeffrose.xio.core;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.xjeffrose.xio.processor.XioProcessor;
import com.xjeffrose.xio.processor.XioProcessorFactory;
import io.airlift.units.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

import static org.junit.Assert.*;

public class NettyServerTransportTest {

  @Test
  public void testStart() throws Exception {
    HttpServerDef serverDef = new HttpServerDefBuilder()
        .clientIdleTimeout(new Duration((double) 200, TimeUnit.MILLISECONDS))
        .limitConnectionsTo(200)
        .limitFrameSizeTo(1024)
        .limitQueuedResponsesPerConnection(50)
        .listen(8082)
        .name("Xio Test Server")
        .taskTimeout(new Duration((double) 20000, TimeUnit.MILLISECONDS))
        .using(Executors.newCachedThreadPool())
        .withSecurityFactory(new XioNoOpSecurityFactory())
        .withProcessorFactory(new XioProcessorFactory() {
          @Override
          public XioProcessor getProcessor() {
            return new XioProcessor() {
              @Override
              public ListenableFuture<Boolean> process(ChannelHandlerContext ctx, HttpRequest req, RequestContext respCtx, Map<Integer, HttpMessage> responseMap) {
                ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
                ListenableFuture<Boolean> httpResponseFuture = service.submit(new Callable<Boolean>() {
                  public Boolean call() {

                    responseMap.put(1, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));

                    return true;
                  }
                });
                return httpResponseFuture;
              }
            };
          }
        })
        .build();

    NettyServerConfig serverConfig = new NettyServerConfigBuilder()
        .setBossThreadCount(12)
        .setBossThreadExecutor(Executors.newCachedThreadPool())
        .setWorkerThreadCount(20)
        .setWorkerThreadExecutor(Executors.newCachedThreadPool())
        .setTimer(new XioTimer("Test Timer", (long) 100, TimeUnit.MILLISECONDS, 100))
        .setXioName("Xio Name Test")
        .build();

    // Create the server transport
    final NettyServerTransport server = new NettyServerTransport(serverDef,
        serverConfig,
        new DefaultChannelGroup());

    // Start the server
    server.start();

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
