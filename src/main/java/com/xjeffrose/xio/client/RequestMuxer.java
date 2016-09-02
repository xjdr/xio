package com.xjeffrose.xio.client;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.PlatformDependent;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
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

// TODO(CK): consider renaming this to something not including Request
@Slf4j
public class RequestMuxer implements AutoCloseable {
  // TODO(CK): remove
  private static final int CONST = 1618;

  // TODO(CK): this isn't being used
  private final AtomicInteger MULT = new AtomicInteger();
  // TODO(CK): this should be a method
  private final int HIGH_WATER_MARK = CONST * MULT.get();

  private final int messagesPerBatch;
  private final Duration drainMessageQInterval;
  private final Duration multiplierIncrementInterval;
  private final Duration multiplierDecrementInterval;
  private final Duration rebuildConnectionLoopInterval;

  private final EventLoopGroup workerLoop;
  private final RequestMuxerConnectionPool connectionPool;
  private final AtomicBoolean isRunning = new AtomicBoolean();

  private final Queue<MuxedMessage> messageQ = Queues.newConcurrentLinkedQueue();

  private AtomicLong counter = new AtomicLong();
  private List<ScheduledFuture> scheduledFutures = Collections.synchronizedList(new ArrayList());

  public RequestMuxer(Config config, EventLoopGroup workerLoop, RequestMuxerConnectionPool connectionPool) {
    messagesPerBatch = config.getInt("messagesPerBatch");
    drainMessageQInterval = config.getDuration("drainMessageQInterval");
    multiplierIncrementInterval = config.getDuration("multiplierIncrementInterval");
    multiplierDecrementInterval = config.getDuration("multiplierDecrementInterval");
    rebuildConnectionLoopInterval = config.getDuration("rebuildConnectionLoopInterval");
    this.workerLoop = workerLoop;
    this.connectionPool = connectionPool;
  }

  private void schedule(Duration interval, Runnable runnable) {
    ScheduledFuture f = workerLoop.scheduleAtFixedRate(runnable, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    scheduledFutures.add(f);
  }

  public void start() throws Exception {
    connectionPool.start();
    isRunning.set(true);

    schedule(drainMessageQInterval, () -> {
      if (counter.get() > 0) {
        drainMessageQ();
      }
    });

    schedule(multiplierIncrementInterval, () -> {
      // TODO(CK): fix this
      if (counter.get() > HIGH_WATER_MARK) {
        MULT.incrementAndGet();
      }
    });

    schedule(multiplierDecrementInterval, () -> {
      // TODO(CK): fix this
      if (counter.get() < HIGH_WATER_MARK / 10) {
        if (MULT.get() > 0) {
          MULT.decrementAndGet();
        }
      }
    });

    schedule(rebuildConnectionLoopInterval, () -> {
      connectionPool.rebuildConnectionQ();
    });
  }

  @Override
  public void close() {
    isRunning.set(false);
    for(ScheduledFuture f : scheduledFutures){
      f.cancel(true);
    }

    // wait for scheduled futures to cancel
    while (scheduledFutures.stream().anyMatch((f) -> !f.isDone())) {
      Uninterruptibles.sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
    }

    // handle remaining items in the queue
    while (counter.get() > 0) {
      drainMessageQ();
    }

    connectionPool.close();
  }

  public ListenableFuture<Void> write(Object request) {
    return write(request, SettableFuture.create());
  }

  public ListenableFuture<Void> write(Object sendReq, SettableFuture<Void> f) {
    if (!isRunning.get()) {
      f.setException(new IllegalStateException("RequestMuxer has not been started"));
      return f;
    }

    // TODO(CK): fix this
    if (counter.incrementAndGet() > HIGH_WATER_MARK) {
      messageQ.add(new MuxedMessage(sendReq, f));
    } else {
      writeMessage(sendReq, f);
    }
    return f;
  }

  private ChannelFutureListener newWriteListener(SettableFuture<Void> promise) {
    return new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          promise.set(null);
        } else {
          promise.setException(future.cause());
        }
      }
    };
  }

  private void drainMessageQ() {
    Optional<Channel> maybeChannel = connectionPool.requestNode();
    maybeChannel.ifPresent((ch) -> {
      int count = 0;
      for (int i = 0; i < messagesPerBatch; i++) {
        // TODO(CK): check if channel is still writeable
        final MuxedMessage mm = messageQ.poll();
        if (mm == null) {
          // we've exhausted the queue
          break;
        }

        count++;
        ch.write(mm.getMsg()).addListener(newWriteListener(mm.getF()));
      }
      // flush here instead of calling writeAndFlush inside of the for loop
      // this way we queue up a series of writes and flush them all at once
      ch.flush();
      final int written = count;
      counter.updateAndGet(i -> i - written);
    });
  }

  private void writeMessage(Object sendReq, SettableFuture<Void> f) {
    Optional<Channel> maybeChannel = connectionPool.requestNode();
    if (maybeChannel.isPresent()) {
      maybeChannel.get().writeAndFlush(sendReq).addListener(newWriteListener(f));
      counter.decrementAndGet();
    } else {
      // No channel available, queue this write
      messageQ.add(new MuxedMessage(sendReq, f));
    }
  }

  @Value
  private class MuxedMessage {
    private final Object msg;
    private final SettableFuture<Void> f;
  }

}
