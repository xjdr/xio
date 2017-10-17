package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xjeffrose.xio.mux.ClientCodec;
import com.xjeffrose.xio.mux.ConnectionPool;
import com.xjeffrose.xio.mux.LocalConnector;
import com.xjeffrose.xio.mux.Request;
import com.xjeffrose.xio.mux.Response;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RequestMuxerUnitTest extends Assert {

  @Setter
  boolean success = false;
  @Setter
  boolean failure = false;

  ConnectionPool connectionPool;

  RequestMuxer requestMuxer;

  List<EmbeddedChannel> channels = new ArrayList<>();

  EventLoopGroup group;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws Exception {
    Config config = ConfigFactory.load().getConfig("xio.testApplication.settings.requestMuxer");
    LocalConnector connector = new LocalConnector("test-muxer") {
      @Override
      public ListenableFuture<Channel> connect() {
        SettableFuture<Channel> result = SettableFuture.create();
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(new ClientCodec());
        channels.add(channel);
        result.set(channel);
        return result;
      }
    };
    // TODO(CK): Override connection pool request node instead of connector.connect
    connectionPool = new ConnectionPool(connector);

    group = new NioEventLoopGroup(5,
      new ThreadFactoryBuilder()
      .setNameFormat("chicagoClient-nioEventLoopGroup-%d")
      .build()
    );

    requestMuxer = new RequestMuxer(
      config,
      group,
      connectionPool
    );
    requestMuxer.start();
  }

  @Test
  public void writeTest() throws Exception {
    Integer payload = new Integer(1);
    Request request = requestMuxer.write(payload);
    CountDownLatch done = new CountDownLatch(1);
    Futures.addCallback(request.getWriteFuture(), new FutureCallback<UUID>() {
      @Override
      public void onSuccess(UUID id) {
        setSuccess(true);
        done.countDown();
      }

      @Override
      public void onFailure(Throwable throwable) {
        setFailure(true);
        done.countDown();
      }
    });

    EmbeddedChannel channel = channels.get(0);
    channel.runPendingTasks();
    Uninterruptibles.awaitUninterruptibly(done); // block
    channel.runPendingTasks();
    assertTrue(request.getWriteFuture().isDone());
    assertFalse(failure);
    assertTrue(success);
    Integer written = (Integer)channel.outboundMessages().peek();
    assertEquals(payload, written);
    requestMuxer.close();
  }

  // TODO(CK): Refactor this into a helper class
  public <T> Optional<T> maybeGetFrom(Future<T> future) {
    try {
      return Optional.ofNullable(Uninterruptibles.getUninterruptibly(future)); // block
    } catch(ExecutionException e) {
      return Optional.empty();
    }
  }

  // TODO(CK): Refactor this into a helper class
  public <T> void blockingCallback(ListenableFuture<T> future, FutureCallback<T> callback) {
    CountDownLatch done = new CountDownLatch(1);
    Futures.addCallback(future, new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
        callback.onSuccess(result);
        done.countDown();
      }

      @Override
      public void onFailure(Throwable throwable) {
        callback.onFailure(throwable);
        done.countDown();
      }
    });
    Uninterruptibles.awaitUninterruptibly(done); // block
  }

  // TODO(CK): Refactor this into a functional test
  @Test
  public void responseTest() {
    Integer payload = new Integer(1);
    Request request = requestMuxer.writeExpectResponse(payload);
    CountDownLatch done = new CountDownLatch(1);
    EmbeddedChannel channel = channels.get(0);
    Optional<UUID> result = maybeGetFrom(request.getWriteFuture());
    assertTrue(result.isPresent());
    result.ifPresent(uuid -> {
      // simulate write response
      Response response = new Response(uuid, payload);
      request.getResponsePromise().set(response);
      // add callback when the response future comes in
      blockingCallback(request.getResponseFuture(), new FutureCallback<Response>() {
        @Override
        public void onSuccess(Response response) {
          setSuccess(true);
        }

        @Override
        public void onFailure(Throwable throwable) {
          setFailure(true);
        }
      }); // block
    });
    assertTrue(success);
    assertFalse(failure);
  }

  // testWrite(payload)
  // assert request has the correct op

  // testWriteExpectResponse(payload)
  // assert request has the correct op

  // testWriteMessageHaveChannel
  // assert message written

  // testWriteMessageNoChannel
  // assert message queued

  // testDrainMessageQ
  // assert no more than X messages processed

  // testWriteOrQueueWrites
  // assert message written

  // testWriteOrQueueQueues
  // assert message queued

  // testWriteNotRunning
  // assert promise fails

  // testCloseStopsRunning
  // testCloseCancelsScheduledTasks
  // testCloseDrainsMessageQ
  // testCloseConnectionPoolClosed

}
