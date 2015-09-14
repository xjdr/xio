package com.xjeffrose.xio.guice;

import com.google.inject.Guice;
import com.google.inject.Stage;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.xjeffrose.xio.core.XioCodecFactory;
import com.xjeffrose.xio.core.XioNoOpSecurityFactory;
import com.xjeffrose.xio.fixtures.XioTestProcessorFactory;
import com.xjeffrose.xio.server.XioBootstrap;
import com.xjeffrose.xio.server.XioServerDef;
import com.xjeffrose.xio.server.XioServerDefBuilder;
import io.airlift.units.Duration;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpServerCodec;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XioModuleTest {

  @Test
  public void testGuice() throws Exception {

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
                .listen(new InetSocketAddress(8086))
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

            // Bind the definition
            bind().toInstance(serverDef);
          }
        }).getInstance(XioBootstrap.class);

    // Start the server
    bootstrap.start();

    // Use 3rd party client to test proper operation
    Request request = new Request.Builder()
        .url("http://127.0.0.1:8086/")
        .build();

    OkHttpClient client = new OkHttpClient();
    Response response = client.newCall(request).execute();

    String expectedResponse = "WELCOME TO THE WILD WILD WEB SERVER\r\n" +
        "===================================\r\n" +
        "VERSION: HTTP/1.1\r\n" +
        "HOSTNAME: 127.0.0.1:8086\r\n" +
        "REQUEST_URI: /\r\n" +
        "\r\n" +
        "HEADER: Host = 127.0.0.1:8086\r\n" +
        "HEADER: Connection = Keep-Alive\r\n" +
        "HEADER: Accept-Encoding = gzip\r\n" +
        "HEADER: User-Agent = okhttp/2.4.0\r\n\r\n";

    assertTrue(response.isSuccessful());
    assertEquals(200, response.code());
    assertEquals(expectedResponse, response.body().string());

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