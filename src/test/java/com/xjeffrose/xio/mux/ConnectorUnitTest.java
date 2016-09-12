package com.xjeffrose.xio.mux;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class ConnectorUnitTest extends Assert {

  LocalServerChannel server;

  LocalAddress address = new LocalAddress("test-location");

  @Before
  public void setUp() {
    EventLoopGroup serverGroup = new DefaultEventLoopGroup();
    ServerBootstrap sb = new ServerBootstrap();
    sb.group(serverGroup)
      .channel(LocalServerChannel.class)
      .handler(new ChannelInitializer<LocalServerChannel>() {
        @Override
        public void initChannel(LocalServerChannel ch) throws Exception {
        }
      })
      .childHandler(new ChannelInitializer<LocalChannel>() {
        @Override
        public void initChannel(LocalChannel ch) throws Exception {
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
  public void testConnect() throws ExecutionException {
    EventLoopGroup group = new DefaultEventLoopGroup();
    Connector connector = new Connector(address) {
      @Override
      protected ChannelHandler responseHandler() {
        return new ChannelInboundHandlerAdapter();
      }

      @Override
      protected EventLoopGroup group() {
        return group;
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
  }

}
