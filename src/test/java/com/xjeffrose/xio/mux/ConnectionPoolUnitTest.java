package com.xjeffrose.xio.mux;

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
