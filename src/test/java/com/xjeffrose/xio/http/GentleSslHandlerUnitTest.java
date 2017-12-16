package com.xjeffrose.xio.http;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.xjeffrose.xio.SSL.SelfSignedX509CertGenerator;
import com.xjeffrose.xio.SSL.X509Certificate;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Test;

public class GentleSslHandlerUnitTest extends Assert {

  public static ByteBuf encodeRequest(HttpRequest request) {
    EmbeddedChannel channel = new EmbeddedChannel();

    channel.pipeline().addLast("http request encoder", new HttpRequestEncoder());
    channel.writeOutbound(request);
    channel.runPendingTasks();
    return channel.readOutbound();
  }

  @Test
  public void testHttpRequest() throws Exception {
    assertTrue(OpenSsl.isAvailable());
    X509Certificate selfSignedCert = SelfSignedX509CertGenerator.generate("*.test.com");

    java.security.cert.X509Certificate[] chain = new java.security.cert.X509Certificate[1];
    chain[0] = selfSignedCert.getCert();

    SslContext sslContext =
        SslContextBuilder.forServer(selfSignedCert.getKey(), chain)
            .sslProvider(SslProvider.OPENSSL)
            .build();
    HttpsUpgradeHandler cleartextHandler = new HttpsUpgradeHandler();
    GentleSslHandler upgradeHandler = new GentleSslHandler(sslContext, cleartextHandler);

    ByteBuf rawRequest = encodeRequest(new DefaultHttpRequest(HTTP_1_1, GET, "/"));

    EmbeddedChannel channel = new EmbeddedChannel();
    channel.pipeline().addLast("upgradeHandler", upgradeHandler);

    channel.writeInbound(rawRequest);

    channel.runPendingTasks(); // blocks

    HttpResponse response = Recipes.decodeResponse(Recipes.extractBuffers(channel));

    assertTrue(response != null);

    assertEquals(response.status(), UPGRADE_REQUIRED);
    assertThat(
        Arrays.asList(response.headers().get(HttpHeaderNames.CONNECTION).split(",")),
        IsCollectionContaining.hasItem(HttpHeaderValues.CLOSE.toString()));
    assertThat(
        Arrays.asList(response.headers().get(HttpHeaderNames.CONNECTION).split(",")),
        IsCollectionContaining.hasItem(HttpHeaderValues.UPGRADE.toString()));
    assertThat(
        Arrays.asList(response.headers().get(HttpHeaderNames.UPGRADE).split(",")),
        IsCollectionContaining.hasItem("TLS/1.2"));
    assertThat(
        Arrays.asList(response.headers().get(HttpHeaderNames.UPGRADE).split(",")),
        IsCollectionContaining.hasItem("HTTP/1.1"));
  }

  @Test
  public void testHttpsRequest() throws Exception {
    assertTrue(OpenSsl.isAvailable());
    // setup server
    X509Certificate selfSignedCert = SelfSignedX509CertGenerator.generate("*.test.com");

    java.security.cert.X509Certificate[] chain = new java.security.cert.X509Certificate[1];
    chain[0] = selfSignedCert.getCert();

    SslContext sslContext =
        SslContextBuilder.forServer(selfSignedCert.getKey(), chain)
            .sslProvider(SslProvider.OPENSSL)
            .build();

    HttpsUpgradeHandler cleartextHandler = new HttpsUpgradeHandler();
    GentleSslHandler upgradeHandler = new GentleSslHandler(sslContext, cleartextHandler);

    final CountDownLatch serverChannelLatch = new CountDownLatch(1);
    final CountDownLatch requestLatch = new CountDownLatch(1);
    LocalAddress serverAddress = new LocalAddress(getClass().getName());

    EventLoopGroup group = new DefaultEventLoop();
    ServerBootstrap sb =
        new ServerBootstrap()
            .channel(LocalServerChannel.class)
            .group(group)
            .childHandler(
                new ChannelInitializer<Channel>() {
                  @Override
                  protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline()
                        .addLast("gentle ssl handler", upgradeHandler)
                        .addLast("http request decoder", new HttpRequestDecoder())
                        .addLast("http message aggregator", new HttpObjectAggregator(1048576))
                        .addLast(
                            "app logic",
                            new SimpleChannelInboundHandler<Object>() {
                              @Override
                              protected void channelRead0(ChannelHandlerContext ctx, Object message)
                                  throws Exception {
                                requestLatch.countDown();
                                assertTrue(
                                    "message is HttpRequest", message instanceof HttpRequest);
                              }
                            });
                    ;

                    serverChannelLatch.countDown();
                  }
                });
    sb.bind(serverAddress).sync();

    // setup client
    SslContext clientContext =
        SslContextBuilder.forClient()
            .sslProvider(SslProvider.OPENSSL)
            .trustManager(selfSignedCert.getCert())
            .build();

    Bootstrap cb =
        new Bootstrap()
            .channel(LocalChannel.class)
            .group(group)
            .handler(
                new ChannelInitializer<Channel>() {
                  @Override
                  protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline()
                        .addLast("ssl client handler", clientContext.newHandler(ch.alloc()))
                        .addLast("http request decoder", new HttpRequestEncoder());
                  }
                });

    Channel clientChannel = cb.connect(serverAddress).sync().channel();
    assertTrue(serverChannelLatch.await(5, SECONDS));
    clientChannel.writeAndFlush(new DefaultHttpRequest(HTTP_1_1, GET, "/"));
    assertTrue(requestLatch.await(5, SECONDS));
  }
}
