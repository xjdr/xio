package com.xjeffrose.xio.mux;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import io.netty.channel.Channel;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import javax.annotation.Nullable;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ConnectionPool implements AutoCloseable {
  // TODO(CK): move to config
  private static final int POOL_SIZE = 4;

  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  private final Deque<Channel> connectionQ = PlatformDependent.newConcurrentDeque();

  private final Connector connector;

  private AtomicBoolean connectionRebuild = new AtomicBoolean(false);

  public ConnectionPool(Connector connector) {
    this.connector = connector;
  }

  public void start() {
    CountDownLatch done = new CountDownLatch(POOL_SIZE);
    FutureCallback<Channel> callback = new FutureCallback<Channel>() {
      @Override
      public void onSuccess(Channel channels) {
        done.countDown();
      }

      // TODO(CK): this error needs to get bubbled back up to the requestor
      @Override
      public void onFailure(Throwable throwable) {
        done.countDown();
      }
    };
    buildInitialConnectionQ(callback);

    // TODO(CK): handle failures and retry
    // block until all of the connections have been established
    Uninterruptibles.awaitUninterruptibly(done);
    if (connectionQ.size() == 0) {
      throw new RuntimeException("Couldn't open any connections");
    }

    isRunning.set(true);
  }

  @Override
  public void close() {
    isRunning.set(false);
    for (Channel channel : connectionQ) {
      channel.close();
    }
  }

  private void buildInitialConnectionQ(FutureCallback<Channel> callback) {
    for (int i = 0; i < POOL_SIZE; i++) {
      ListenableFuture<Channel> result = connector.connect();
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
      Futures.addCallback(result, callback);
    }
  }

  public void rebuildConnectionQ() {
    if (connectionRebuild.get() == false) {
      return;
    }

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

    connectionRebuild.set(false);
  }

  public Optional<Channel> requestNode(){
    Channel channel = connectionQ.pollFirst();

    // TODO(CK): should we check for isWriteable?
    if (channel != null && channel.isActive()) {
      connectionQ.addLast(channel);
      return Optional.of(channel);
    }

    connectionRebuild.set(true);
    return Optional.empty();
  }

}
