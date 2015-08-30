package com.xjeffrose.xio.guice;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import com.google.inject.Stage;
import com.xjeffrose.xio.core.XioCodecFactory;
import com.xjeffrose.xio.core.XioNoOpSecurityFactory;
import com.xjeffrose.xio.processor.XioProcessor;
import com.xjeffrose.xio.processor.XioProcessorFactory;
import com.xjeffrose.xio.server.RequestContext;
import com.xjeffrose.xio.server.XioServerConfig;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerDefBuilder;
import com.xjeffrose.xio.server.XioBootstrap;
import io.airlift.units.Duration;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class XioModuleTest {

  @Test
  public void testBind() throws Exception {

    final XioBootstrap bootstrap = Guice.createInjector(
        Stage.PRODUCTION,
        new XioModule() {
          @Override
          protected void configureXio() {
//              // Build the server definition
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

            // Bind the definition
            bind().toInstance(serverDef);
          }
        }).getInstance(XioBootstrap.class);

    // Start the server
    bootstrap.start();

    //For testing only (LEAVE OUT)
//    Thread.sleep(200000000);

    // Arrange to stop the server at shutdown
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        bootstrap.stop();
      }
    });

  }
}