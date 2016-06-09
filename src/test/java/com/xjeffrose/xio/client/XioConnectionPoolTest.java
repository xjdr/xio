package com.xjeffrose.xio.client;

import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoop;
import com.xjeffrose.xio.client.asyncretry.AsyncRetryLoopFactory;
import java.nio.charset.Charset;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Future;
import org.junit.*;
import static org.junit.Assert.*;

public class XioConnectionPoolTest {
  private static final String LOCAL_ADDR_ID = "test.id";

  @Test
  public void testConnectionFailsNoRetry() {
    EventLoopGroup group = new LocalEventLoopGroup();
    Bootstrap bootstrap = new Bootstrap()
      .group(group)
      .channel(LocalChannel.class)
      .handler(new ChannelInboundHandlerAdapter())
      .remoteAddress(LocalAddress.ANY)
    ;
    AsyncRetryLoopFactory factory = new AsyncRetryLoopFactory() {
      public AsyncRetryLoop buildLoop(EventLoopGroup eventLoopGroup) {
        return new AsyncRetryLoop(0, eventLoopGroup, 1, TimeUnit.MILLISECONDS);
      }
    };
    XioConnectionPool pool = new XioConnectionPool(bootstrap, factory);
    Future<Channel> f = pool.acquire();
    f.awaitUninterruptibly();
    assertFalse(f.isSuccess());
  }

  @Test
  public void testConnectionFailsAfterRetry() {
    EventLoopGroup group = new LocalEventLoopGroup();
    Bootstrap bootstrap = new Bootstrap()
      .group(group)
      .channel(LocalChannel.class)
      .handler(new ChannelInboundHandlerAdapter())
      .remoteAddress(LocalAddress.ANY)
    ;
    AsyncRetryLoopFactory factory = new AsyncRetryLoopFactory() {
      public AsyncRetryLoop buildLoop(EventLoopGroup eventLoopGroup) {
        return new AsyncRetryLoop(3, eventLoopGroup, 1, TimeUnit.MILLISECONDS);
      }
    };
    XioConnectionPool pool = new XioConnectionPool(bootstrap, factory);
    Future<Channel> f = pool.acquire();
    f.awaitUninterruptibly();
    assertFalse(f.isSuccess());
  }

  @Test
  public void testConnectionSucceedsAfterRetry() {
    Bootstrap bootstrap = new Bootstrap() {
      int connectCount = 0;
      public ChannelFuture connect() {
        connectCount++;
        if (connectCount == 1) {
          DefaultChannelPromise result = new DefaultChannelPromise(null);
          result.setFailure(new RuntimeException());
          return result;
        } else {
          return super.connect();
        }
      }
    };
    EventLoopGroup group = new LocalEventLoopGroup();
    LocalAddress addr = new LocalAddress(LOCAL_ADDR_ID);
    bootstrap
      .group(group)
      .channel(LocalChannel.class)
      .handler(new ChannelInboundHandlerAdapter())
      .remoteAddress(addr)
    ;
    ServerBootstrap sb = new ServerBootstrap()
      .group(group)
      .channel(LocalServerChannel.class)
      .childHandler(new ChannelInboundHandlerAdapter())
    ;

    Channel sc = sb.bind(addr).syncUninterruptibly().channel();
    AsyncRetryLoopFactory factory = new AsyncRetryLoopFactory() {
      public AsyncRetryLoop buildLoop(EventLoopGroup eventLoopGroup) {
        return new AsyncRetryLoop(3, eventLoopGroup, 1, TimeUnit.MILLISECONDS);
      }
    };
    XioConnectionPool pool = new XioConnectionPool(bootstrap, factory);
    Future<Channel> f = pool.acquire();
    f.awaitUninterruptibly();
    assertTrue(f.isSuccess());
  }
}
