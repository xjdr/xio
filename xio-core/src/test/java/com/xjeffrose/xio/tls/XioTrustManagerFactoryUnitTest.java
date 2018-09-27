package com.xjeffrose.xio.tls;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertPathValidatorException;
import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class XioTrustManagerFactoryUnitTest extends Assert {

  private static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("127.0.0.1", 0);
  private InetSocketAddress boundAddress;
  private EventLoopGroup group;
  private SslContext sslServerContext;
  private SslContext sslClientContext;
  private ServerBootstrap server;
  private Bootstrap client;
  private Channel serverChannel;
  private CountDownLatch msgReceived = new CountDownLatch(1);
  private HeldCertificate rootCa;
  private HeldCertificate serverCert;
  private HeldCertificate clientCert;

  @Before
  public void setUp() throws Exception {
    rootCa = new HeldCertificate.Builder().serialNumber("1").ca(3).commonName("root").build();
    serverCert =
        new HeldCertificate.Builder()
            .issuedBy(rootCa)
            .serialNumber("2")
            .commonName("127.0.0.1")
            .build();
  }

  @After
  public void tearDown() {
    group.shutdownGracefully(0, 1000, TimeUnit.MILLISECONDS).syncUninterruptibly();
  }

  @Test
  public void testExpiredCertificateNotAllowed() throws Exception {
    Instant now = Instant.now();
    Period aWeek = Period.ofDays(7);
    clientCert =
        new HeldCertificate.Builder()
            .issuedBy(rootCa)
            .serialNumber("3")
            .commonName("Test Client")
            .notBefore(Date.from(now.minus(aWeek)))
            .notAfter(Date.from(now.minus(aWeek.minusDays(1))))
            .build();
    group = new NioEventLoopGroup(2);
    Config config = ConfigFactory.load();
    TlsConfig serverConfig =
        TlsConfig.builderFrom(config.getConfig("xio.testServer.settings.tls"))
            .certificate(serverCert.certificate)
            .certChain(ImmutableList.of(rootCa.certificate))
            .privateKey(serverCert.keyPair.getPrivate())
            .trustedCerts(ImmutableList.of(rootCa.certificate))
            .build();
    TlsConfig clientConfig =
        TlsConfig.builderFrom(config.getConfig("xio.h1TestClient.settings.tls"))
            .certificate(clientCert.certificate)
            .certChain(ImmutableList.of(rootCa.certificate))
            .privateKey(clientCert.keyPair.getPrivate())
            .trustedCerts(ImmutableList.of(rootCa.certificate))
            .build();
    sslServerContext = SslContextFactory.buildServerContext(serverConfig);
    sslClientContext = SslContextFactory.buildClientContext(clientConfig);

    server =
        new ServerBootstrap()
            .group(group)
            .channel(NioServerSocketChannel.class)
            .childHandler(
                new ChannelInitializer<Channel>() {
                  @Override
                  protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline()
                        .addLast(sslServerContext.newHandler(ch.alloc()))
                        .addLast(new MutualAuthHandler())
                        .addLast(
                            new ChannelInboundHandlerAdapter() {
                              @Override
                              public void channelRead(ChannelHandlerContext ctx, Object msg)
                                  throws Exception {
                                assertEquals(
                                    "client is not authenticated",
                                    TlsAuthState.UNAUTHENTICATED,
                                    TlsAuthState.getPeerIdentity(ctx));
                                ctx.writeAndFlush(msg);
                              }

                              @Override
                              public void exceptionCaught(
                                  ChannelHandlerContext ctx, Throwable cause) {
                                assertTrue(cause instanceof DecoderException);
                                assertTrue(cause.getCause() instanceof SSLHandshakeException);
                                assertTrue(
                                    cause.getCause().getCause()
                                        instanceof sun.security.validator.ValidatorException);
                                assertTrue(
                                    cause.getCause().getCause().getCause()
                                        instanceof CertPathValidatorException);
                                ctx.close();

                                /*
                                System.out.println("server ctx: " + ctx + " cause: " + cause);
                                System.out.println("server cause cause: " + cause.getCause() + " suppressed: " + Arrays.asList(cause.getSuppressed()));
                                System.out.println("server cause cause cause: " + cause.getCause().getCause() + " suppressed: " + Arrays.asList(cause.getCause().getSuppressed()));
                                System.out.println("server cause cause cause cause: " + cause.getCause().getCause().getCause());
                                */
                              }
                            });
                  }
                })
            .localAddress(SERVER_ADDRESS);

    serverChannel = server.bind().syncUninterruptibly().channel();

    boundAddress = (InetSocketAddress) serverChannel.localAddress();

    client =
        new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(
                new ChannelInitializer<Channel>() {
                  @Override
                  protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline()
                        .addLast(
                            sslClientContext.newHandler(
                                ch.alloc(), boundAddress.getHostString(), boundAddress.getPort()))
                        .addLast(
                            new ChannelInboundHandlerAdapter() {
                              @Override
                              public void channelRead(ChannelHandlerContext ctx, Object msg)
                                  throws Exception {
                                msgReceived.countDown();
                              }

                              @Override
                              public void exceptionCaught(
                                  ChannelHandlerContext ctx, Throwable cause) {
                                assertTrue(cause instanceof DecoderException);
                                assertTrue(cause.getCause() instanceof SSLHandshakeException);
                                ctx.close();
                                msgReceived.countDown();
                                /*
                                System.out.println("ctx: " + ctx + " cause: " + cause);
                                System.out.println("client cause cause: " + cause.getCause() + " suppressed: " + Arrays.asList(cause.getSuppressed()));
                                System.out.println("client cause cause cause: " + cause.getCause().getCause() + " suppressed: " + Arrays.asList(cause.getCause().getSuppressed()));
                                */
                              }
                            });
                  }
                })
            .remoteAddress(boundAddress);

    Channel channel = client.connect().syncUninterruptibly().channel();

    try {
      ByteBuf buf = Unpooled.copiedBuffer("DATA", StandardCharsets.UTF_8);
      channel.writeAndFlush(buf).syncUninterruptibly();
      Uninterruptibles.awaitUninterruptibly(msgReceived);
    } finally {
      serverChannel.close().syncUninterruptibly();
      channel.close().syncUninterruptibly();
    }
  }
}
