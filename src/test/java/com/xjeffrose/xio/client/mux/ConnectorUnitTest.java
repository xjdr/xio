package com.xjeffrose.xio.client.mux;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import com.xjeffrose.xio.core.FrameLengthCodec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectorUnitTest extends Assert {

  LocalServerChannel server;

  LocalAddress address = new LocalAddress("test-location");

  private ChannelHandler newServerHandler() {
    return new ChannelInboundHandlerAdapter() {

      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // Write back as received
        ctx.writeAndFlush(msg);
      }

      @Override
      public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
      }
    };
  }

  @Before
  public void setUp() {
    EventLoopGroup serverGroup = new DefaultEventLoopGroup();
    ServerBootstrap sb = new ServerBootstrap();
    sb.group(serverGroup)
      .channel(LocalServerChannel.class)
      .handler(new ChannelInitializer<LocalServerChannel>() {
        @Override
        public void initChannel(LocalServerChannel ch) throws Exception {
          ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
        }
      })
      .childHandler(new ChannelInitializer<LocalChannel>() {
        @Override
        public void initChannel(LocalChannel ch) throws Exception {
          ch.pipeline()
            .addLast("logging handler", new LoggingHandler(LogLevel.INFO))
            .addLast("frame length codec", new FrameLengthCodec())
            .addLast("muxing protocol codec", new Codec())
            .addLast("server handler", newServerHandler())
            ;
        }
      });
    ChannelFuture future = sb.bind(address);

    future.awaitUninterruptibly();

    server = (LocalServerChannel)future.channel();
  }

  @After
  public void tearDown() {
    server.close();
  }

  @Test
  public void testLocalChannel() throws ExecutionException {
    AtomicReference<Message> responseMessage = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    Connector connector = new Connector(address) {
      @Override
      protected ChannelHandler responseHandler() {
        return new SimpleChannelInboundHandler<Message>() {

          @Override
          public void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
            responseMessage.set(msg);
            latch.countDown();
          }

          @Override
          public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
            latch.countDown();
          }
        };
      }

      @Override
      protected EventLoopGroup group() {
        return new LocalEventLoopGroup();
      }

      @Override
      protected Class<? extends Channel> channel () {
        return LocalChannel.class;
      }
    };

    ListenableFuture<Channel> future = connector.connect();

    CountDownLatch done = new CountDownLatch(1);

    Futures.addCallback(future, new FutureCallback<Channel>() {
      @Override
      public void onSuccess(Channel ch) {
        assertTrue(true);
        done.countDown();
      }

      @Override
      public void onFailure(Throwable throwable) {
        done.countDown();
        assertTrue(false);
      }
    });

    Uninterruptibles.awaitUninterruptibly(done); // block

    Message message = new Message();
    Channel client = Uninterruptibles.getUninterruptibly(future);
    client.writeAndFlush(message).awaitUninterruptibly();

    boolean noTimeout = Uninterruptibles.awaitUninterruptibly(latch, 2, TimeUnit.SECONDS); // block
    assertTrue(noTimeout);

    assertEquals(message, responseMessage.get());
  }

}
