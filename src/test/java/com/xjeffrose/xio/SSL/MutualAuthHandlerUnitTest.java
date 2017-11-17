package com.xjeffrose.xio.SSL;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import com.google.common.util.concurrent.Uninterruptibles;
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
import io.netty.handler.ssl.SslContext;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MutualAuthHandlerUnitTest extends Assert {

  private static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("127.0.0.1", 0);
  private InetSocketAddress boundAddress;
  private EventLoopGroup group;
  private SslContext sslServerContext;
  private SslContext sslClientContext;
  private ServerBootstrap server;
  private Bootstrap client;
  private Channel serverChannel;
  private CountDownLatch msgReceived = new CountDownLatch(1);

  @Before
  public void setUp() throws Exception {
    group = new NioEventLoopGroup(2);
    sslServerContext = SslContextFactory.buildServerContext(TlsConfig.fromConfig("xio.testServer.settings.tls"));
    sslClientContext = SslContextFactory.buildClientContext(TlsConfig.fromConfig("xio.h1TestClient.settings.tls"));

    server = new ServerBootstrap()
      .group(group)
      .channel(NioServerSocketChannel.class)
      .childHandler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.pipeline()
              .addLast(sslServerContext.newHandler(ch.alloc()))
              .addLast(new MutualAuthHandler())
              .addLast(new ChannelInboundHandlerAdapter() {
                  @Override
                  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    assertNotEquals("client is not authenticated", TlsAuthState.UNAUTHENTICATED, TlsAuthState.getPeerIdentity(ctx));
                    ctx.writeAndFlush(msg);
                  }
                }
              )
            ;
          }
        })
      .localAddress(SERVER_ADDRESS);

    serverChannel = server.bind().syncUninterruptibly().channel();

    boundAddress = (InetSocketAddress)serverChannel.localAddress();

    client = new Bootstrap()
      .group(group)
      .channel(NioSocketChannel.class)
      .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.pipeline()
            .addLast(sslClientContext.newHandler(ch.alloc(), boundAddress.getHostString(), boundAddress.getPort()))
              .addLast(new ChannelInboundHandlerAdapter() {
                  @Override
                  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    msgReceived.countDown();
                  }
                }
              )
            ;
          }
        })
      .remoteAddress(boundAddress)
      ;
  }

  @After
  public void tearDown() {
    group.shutdownGracefully(0, 1000, TimeUnit.MILLISECONDS).syncUninterruptibly();
  }


  @Test
  public void testMutualAuth() throws Exception {

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
