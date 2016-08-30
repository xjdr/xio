package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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

public class RequestMuxerConnectionPoolUnitTest extends Assert {
  RequestMuxerConnectionPool.Connector connector;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws Exception {
    connector = new RequestMuxerConnectionPool.Connector() {
      @Override
      public ListenableFuture<Channel> connect() {
        SettableFuture<Channel> result = SettableFuture.create();
        result.set(new EmbeddedChannel());
        return result;
      }
    };
  }

  @Test
  public void connectSucceeds() throws Exception {
    RequestMuxerConnectionPool pool = new RequestMuxerConnectionPool(connector);
    pool.start();
  }

  /*
  @Test(expected=RuntimeException.class)
  public void connectFails() {
    RequestMuxerConnectionPool.Connector flakyConnector = new RequestMuxerConnectionPool.Connector() {
      @Override
      public ListenableFuture<Channel> connect() {
        SettableFuture<Channel> result = SettableFuture.create();
        result.setException(new RuntimeException("bad hair day"));
        return result;
      }
    };
    RequestMuxerConnectionPool pool = new RequestMuxerConnectionPool(flakyConnector);
    pool.start();
  }
  */

}
