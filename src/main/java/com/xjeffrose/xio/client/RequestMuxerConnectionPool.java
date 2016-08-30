package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.PlatformDependent;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RequestMuxerConnectionPool implements AutoCloseable {
  // TODO(CK): move to config
  private static final int POOL_SIZE = 4;

  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  private final Deque<Channel> connectionQ = PlatformDependent.newConcurrentDeque();

  public interface Connector {
    ListenableFuture<Channel> connect();
  }

  private final Connector connector;
  private AtomicBoolean connectionRebuild = new AtomicBoolean(false);

  public RequestMuxerConnectionPool(Connector connector) {
    this.connector = connector;
  }

  public void start() {
    try {
      ListenableFuture<List<Channel>> result = buildInitialConnectionQ();
      // TODO(CK): handle failures and retry
      result.get(); // block until all of the connections have been established
      isRunning.set(true);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    isRunning.set(false);
    // TODO(CK): close open connections
  }

  private ListenableFuture<List<Channel>> buildInitialConnectionQ() {
    List<ListenableFuture<Channel>> results = new ArrayList<>();
    for (int i = 0; i < POOL_SIZE; i++) {
      ListenableFuture<Channel> result = connector.connect();
      results.add(result);
      Futures.addCallback(result, new FutureCallback<Channel>() {
        @Override
        public void onSuccess(@Nullable Channel channel) {
          connectionQ.addLast(channel);
        }

        // TODO(CK): this error needs to get bubbled back up to the requestor
        @Override
        public void onFailure(Throwable throwable) {
          log.error("Error connecting to ", throwable);
        }
      });
    }

    return Futures.allAsList(results);
  }

  void rebuildConnectionQ() {
    rebuildConnectionQ(this.connectionQ);
  }

  private void rebuildConnectionQ(Deque<Channel> connectionQ) {
    connectionQ.stream().forEach(xs -> {
      Channel channel = xs;
//      connectionQ.remove(xs);
      // TODO(CK): change this to a not and get rid of the else
      if (channel.isActive()) {
//        connectionQ.addLast(cf);
      } else {
        connectionQ.remove(xs);
        Futures.addCallback(connector.connect(), new FutureCallback<Channel>() {
          @Override
          public void onSuccess(@Nullable Channel channel) {
            connectionQ.addLast(channel);
          }

          // TODO(CK): this error needs to get bubbled back up to the requestor
          @Override
          public void onFailure(Throwable throwable) {
            log.error("Error connecting to ", throwable);
          }
        });
      }
    });
  }

  Optional<Channel> requestNode(){
    Channel channel = connectionQ.pollFirst();

    if (channel != null && channel.isActive()) {
      connectionQ.addLast(channel);
      return Optional.of(channel);
    }

    connectionRebuild.set(true);
    return Optional.empty();
  }

}
