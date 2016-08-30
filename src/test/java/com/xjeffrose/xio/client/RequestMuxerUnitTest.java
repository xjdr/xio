package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

public class RequestMuxerUnitTest extends Assert {

  RequestMuxerConnectionPool connectionPool;

  RequestMuxer requestMuxer;

  EmbeddedChannel channel;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws Exception {
    channel = new EmbeddedChannel();
    Config config = ConfigFactory.load().getConfig("xio.testApplication.settings.requestMuxer");
    RequestMuxerConnectionPool.Connector connector = new RequestMuxerConnectionPool.Connector() {
      @Override
      public ListenableFuture<Channel> connect() {
        SettableFuture<Channel> result = SettableFuture.create();
        result.set(channel);
        return result;
      }
    };
    connectionPool = new RequestMuxerConnectionPool(connector);

    requestMuxer = new RequestMuxer(
      config,
      new NioEventLoopGroup(5,
        new ThreadFactoryBuilder()
         .setNameFormat("chicagoClient-nioEventLoopGroup-%d")
         .build()
      ),
      connectionPool
    );
  }

  @Test
  public void writeTest() throws Exception{
    SettableFuture<ChannelFuture> f = SettableFuture.create();
    ChannelFuture cf = mock(ChannelFuture.class);
    f.set(cf);
    //    when(connector.connect(new InetSocketAddress("127.0.0.1",12000))).thenReturn(f);
    when(cf.isSuccess()).thenReturn(true);

    Channel helper = new EmbeddedChannel(mock(ChannelHandler.class));
    Channel chMock = spy(helper);
    ChannelPromise promise = new DefaultChannelPromise(chMock);

    when(cf.channel()).thenReturn(helper);
    when(chMock.isWritable()).thenReturn(true);
    requestMuxer.start();
    Object payload = new Integer(1);
    SettableFuture<Void> f2 = SettableFuture.create();
    DefaultChannelPromise cfmock = mock(DefaultChannelPromise.class);
    when(chMock.writeAndFlush(payload)).thenReturn(promise);
    promise.setSuccess();
    when(cfmock.isSuccess()).thenReturn(true);
    requestMuxer.write(payload, f2);
    CountDownLatch latch = new CountDownLatch(1);
    Futures.addCallback(f2, new FutureCallback<Void>() {
      @Override
      public void onSuccess(@Nullable Void result) {
        latch.countDown();
      }

      @Override
      public void onFailure(Throwable t) {

      }
    });

    latch.await();
  }

}
