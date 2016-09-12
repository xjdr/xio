package com.xjeffrose.xio.client.mux;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xjeffrose.xio.client.mux.ConnectionPool;
import com.xjeffrose.xio.client.mux.LocalConnector;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ConnectionPool.class, LoggerFactory.class})
public class ConnectionPoolUnitTest extends Assert {
  LocalConnector connector;

  @Before
  public void setUp() throws Exception {
    mockStatic(LoggerFactory.class);
    Logger logger = mock(Logger.class);
    when(LoggerFactory.getLogger(any(Class.class))).thenReturn(logger);

    connector = new LocalConnector("test-connection-pool") {
      @Override
      protected ChannelHandler responseHandler() {
        return null;
      }

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
    ConnectionPool pool = new ConnectionPool(connector);
    pool.start();
  }

  @Test(expected=RuntimeException.class)
  public void connectFails() {
    LocalConnector flakyConnector = new LocalConnector("test-flaky-connection") {
      @Override
      protected ChannelHandler responseHandler() {
        return null;
      }

      @Override
      public ListenableFuture<Channel> connect() {
        SettableFuture<Channel> result = SettableFuture.create();
        result.setException(new RuntimeException("bad hair day"));
        return result;
      }
    };
    ConnectionPool pool = new ConnectionPool(flakyConnector);
    pool.start();
  }

}
