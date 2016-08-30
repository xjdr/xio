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
public class RequestMuxer implements AutoCloseable {
  // TODO(CK): move to config
  private static final int POOL_SIZE = 4;
  // TODO(CK): remove
  private static final int CONST = 1618;

  // TODO(CK): this isn't being used
  private final AtomicInteger MULT = new AtomicInteger();
  // TODO(CK): this should be a method
  private final int HIGH_WATER_MARK = CONST * MULT.get();

  // TODO(CK): this should be a proper address type
  private final String addr;
  private final EventLoopGroup workerLoop;
  private final AtomicBoolean isRunning = new AtomicBoolean();
  // TODO(CK): RequestMuxer should take a ConnectionPool class, this should be in it.
  private final Deque<ChannelFuture> connectionQ = PlatformDependent.newConcurrentDeque();
  private final Deque<MuxedMessage> messageQ = PlatformDependent.newConcurrentDeque();

  public interface Connector {
    // TODO(CK): change this to connect();
    ListenableFuture<ChannelFuture> connect(InetSocketAddress address);
  }
  private final Connector connector;
  private AtomicLong counter = new AtomicLong();
  private AtomicBoolean connectionRebuild = new AtomicBoolean(false);
  private List<ScheduledFuture> scheduledFutures = Collections.synchronizedList(new ArrayList());

  public RequestMuxer(String addr, EventLoopGroup workerLoop, Connector connector) {
    this.addr = addr;
    this.workerLoop = workerLoop;
    this.connector = connector;
  }

  public void start() throws Exception {
    buildInitialConnectionQ();
    blockAndAwaitPool();
    isRunning.set(true);

    // TODO(CK): get time from config
    workerLoop.scheduleAtFixedRate(() -> {
      if (messageQ.size() > 0) {
        drainMessageQ();
      }
    },0,1,TimeUnit.MILLISECONDS);

    // TODO(CK): get time from config
    ScheduledFuture f =  workerLoop.scheduleAtFixedRate(() -> {
        // TODO(CK): fix this
      if (messageQ.size() > HIGH_WATER_MARK) {
        MULT.incrementAndGet();
      }
    },0,500,TimeUnit.MILLISECONDS);
    scheduledFutures.add(f);

    // TODO(CK): get time from config
    f = workerLoop.scheduleAtFixedRate(() -> {
        // TODO(CK): fix this
      if ( messageQ.size() < HIGH_WATER_MARK / 10) {
        if (MULT.get() > 0) {
          MULT.decrementAndGet();
        }
      }
    },0,750,TimeUnit.MILLISECONDS);
    scheduledFutures.add(f);

    // TODO(CK): get time from config
    f = workerLoop.scheduleAtFixedRate(() -> {
      if(connectionRebuild.get()){
        rebuildConnectionQ();
      }
    },0,250,TimeUnit.MILLISECONDS);
    scheduledFutures.add(f);
  }

  @Override
  public void close() {
    isRunning.set(false);
    for(ScheduledFuture f : scheduledFutures){
      f.cancel(true);
    }
    // TODO(CK): handle remaining items in the queue
  }

  // TODO(CK): move to the constructor
  private InetSocketAddress address(String node) {
    String chunks[] = node.split(":");
    return new InetSocketAddress(chunks[0], Integer.parseInt(chunks[1]));
  }

  private void buildInitialConnectionQ() {
    for (int i = 0; i < POOL_SIZE; i++) {
      Futures.addCallback(connector.connect(address(addr)), new FutureCallback<ChannelFuture>() {
        @Override
        public void onSuccess(@Nullable ChannelFuture channelFuture) {
          connectionQ.addLast(channelFuture);

        }

        // TODO(CK): this error needs to get bubbled back up to the requestor
        @Override
        public void onFailure(Throwable throwable) {
          log.error("Error connecting to " + addr, throwable);
        }
      });
    }
  }

  void rebuildConnectionQ() {
    rebuildConnectionQ(this.connectionQ);
  }

  private void rebuildConnectionQ(Deque<ChannelFuture> connectionQ) {
    connectionQ.stream().parallel().forEach(xs -> {
      ChannelFuture cf = xs;
//      connectionQ.remove(xs);
      // TODO(CK): change this to a not and get rid of the else
      if (cf.channel().isActive()) {
//        connectionQ.addLast(cf);
      } else {
        connectionQ.remove(xs);
        Futures.addCallback(connector.connect(address(addr)), new FutureCallback<ChannelFuture>() {
          @Override
          public void onSuccess(@Nullable ChannelFuture channelFuture) {
            connectionQ.addLast(channelFuture);
          }

          // TODO(CK): this error needs to get bubbled back up to the requestor
          @Override
          public void onFailure(Throwable throwable) {
            log.error("Error connecting to " + addr, throwable);
          }
        });
      }
    });
  }

  // TODO(CK): move this into the connection pool class
  private boolean blockAndAwaitPool() {
    // TODO(CK): this is terrible, we should be waiting on a list of futures
    while (connectionQ.size() != POOL_SIZE) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
        return false;
      }
    }

    return true;
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
      messageQ.addLast(new MuxedMessage(sendReq, f));
    } else {
      writeMessage(sendReq, f);
    }
    return f;
  }

  // TODO(CK): move this into a connection pool class
  // TODO(CK): return Future<Channel>
  private Channel requestNode(){

    ChannelFuture cf = connectionQ.removeFirst();

    // TODO(CK): handle if cf is null
//    ChannelFuture cf = connectionQ.peek();
    if ((cf != null) && cf.isSuccess()) {
      if (cf.channel().isActive()) {
        connectionQ.addLast(cf);
        return cf.channel();
      } else {

//        while(!cf.channel().isWritable()){
//          try {
//            Thread.sleep(1);
//          } catch (InterruptedException e) {
//            e.printStackTrace();
//          }
//        }
//        connectionQ.addLast(cf);
        connectionRebuild.set(true);
        // TODO(CK): this is crazy use a Future
        return connectionQ.peekLast().channel();
      }
    } else {
      log.info("Rebuilding connectionQ when channel is not successful!!!");
      connectionRebuild.set(true);
//      return requestNode();
      // TODO(CK): this is crazy use a Future
      return connectionQ.peekLast().channel();
    }
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
    Channel ch = requestNode();
    final int snapshot = messageQ.size(); // TODO(CK): is size guaranteed to return a useful value?
    int count = 0;
    while (snapshot > count) {
      // TODO(CK): handle null
      final MuxedMessage mm = messageQ.pollFirst();
      count++;
      ch.write(mm.getMsg()).addListener(newWriteListener(mm.getF()));
    }
    // TODO(CK): document why the flush is here
    ch.flush();

    counter.updateAndGet((i) -> i - snapshot);
  }

  private void writeMessage(Object sendReq, SettableFuture<Void> f) {
    requestNode().writeAndFlush(sendReq).addListener(newWriteListener(f));
    counter.decrementAndGet();
  }

  @Data
  private class MuxedMessage {
    private final Object msg;
    private final SettableFuture<Void> f;
  }

}
