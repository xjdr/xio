package com.xjeffrose.xio.client;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import com.xjeffrose.xio.mux.ConnectionPool;
import com.xjeffrose.xio.mux.Message;
import com.xjeffrose.xio.mux.Request;
import com.xjeffrose.xio.mux.Response;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.ScheduledFuture;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Value;


// write flow:
//
// write(Payload)
// channel.write(RequestMessage<Payload>)
// RequestEncoder.encode()
// PayloadEncoder.encode()
// Encoder.encode()
// FrameLengthCodec.encode()

// TODO(CK): consider renaming this to something not including Request
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
  private final ConnectionPool connectionPool;
  private final AtomicBoolean isRunning = new AtomicBoolean();

  private final Queue<MuxedMessage> messageQ = Queues.newConcurrentLinkedQueue();

  private AtomicLong counter = new AtomicLong();
  private List<ScheduledFuture<?>> scheduledFutures = Collections.synchronizedList(new ArrayList<>());

  public RequestMuxer(Config config, EventLoopGroup workerLoop, ConnectionPool connectionPool) {
    messagesPerBatch = config.getInt("messagesPerBatch");
    drainMessageQInterval = config.getDuration("drainMessageQInterval");
    multiplierIncrementInterval = config.getDuration("multiplierIncrementInterval");
    multiplierDecrementInterval = config.getDuration("multiplierDecrementInterval");
    rebuildConnectionLoopInterval = config.getDuration("rebuildConnectionLoopInterval");
    this.workerLoop = workerLoop;
    this.connectionPool = connectionPool;
  }

  private void schedule(Duration interval, Runnable runnable) {
    ScheduledFuture<?> f = workerLoop.scheduleAtFixedRate(runnable, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
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
    for(ScheduledFuture<?> f : scheduledFutures){
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

  Request writeOrQueue(Object payload, Request request) {
    if (!isRunning.get()) {
      request.getWritePromise().setException(new IllegalStateException("RequestMuxer has not been started"));
      return request;
    }

    // TODO(CK): fix this
    if (counter.incrementAndGet() > HIGH_WATER_MARK) {
      messageQ.add(new MuxedMessage(payload, request));
    } else {
      writeMessage(payload, request);
    }
    return request;
  }

  public Request write(Object payload) {
    return write(payload, SettableFuture.create());
  }

  public Request write(Object payload, SettableFuture<UUID> writeFuture) {
    Request request = new Request(UUID.randomUUID(), writeFuture);
    return writeOrQueue(payload, request);
  }

  public Request writeExpectResponse(Object payload) {
    return writeExpectResponse(payload, SettableFuture.create(), SettableFuture.create());
  }

  public Request writeExpectResponse(Object payload, SettableFuture<UUID> writeFuture, SettableFuture<Response> responseFuture) {
    Request request = new Request(UUID.randomUUID(), writeFuture, responseFuture);
    return writeOrQueue(payload, request);
  }

  private ChannelFutureListener newWriteListener(SettableFuture<UUID> promise, Request request) {
    return new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          promise.set(request.getId());
        } else {
          promise.setException(future.cause());
        }
      }
    };
  }

  // TODO(CK): split out some of this complexity?
  void drainMessageQ() {
    Optional<Channel> maybeChannel = connectionPool.requestNode();
    maybeChannel.ifPresent((ch) -> {
      int count = 0;
      for (int i = 0; i < messagesPerBatch; i++) {
        if (!ch.isActive() || !ch.isOpen() || !ch.isWritable()) {
          break;
        }
        final MuxedMessage mm = messageQ.poll();
        if (mm == null) {
          // we've exhausted the queue
          break;
        }

        count++;
        Message message = new Message(mm.request, mm.msg);
        ch.write(message).addListener(newWriteListener(mm.request.getWritePromise(), mm.request));
      }
      // flush here instead of calling writeAndFlush inside of the for loop
      // this way we queue up a series of writes and flush them all at once
      ch.flush();
      final int written = count;
      counter.updateAndGet(i -> i - written);
    });
  }

  void writeMessage(Object payload, Request request) {
    Optional<Channel> maybeChannel = connectionPool.requestNode();
    if (maybeChannel.isPresent()) {
      Message message = new Message(request, payload);
      maybeChannel.get().writeAndFlush(message).addListener(newWriteListener(request.getWritePromise(), request));
      counter.decrementAndGet();
    } else {
      // No channel available, queue this write
      messageQ.add(new MuxedMessage(payload, request));
    }
  }

  @Value
  private class MuxedMessage {
    final Object msg;
    final Request request;
  }

}
