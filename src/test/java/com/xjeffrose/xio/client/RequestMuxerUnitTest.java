package com.xjeffrose.xio.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static org.mockito.Mockito.*;

public class RequestMuxerUnitTest extends Assert {

  @Setter
  boolean success = false;
  @Setter
  boolean failure = false;

  RequestMuxerConnectionPool connectionPool;

  RequestMuxer requestMuxer;

  List<EmbeddedChannel> channels = new ArrayList<>();

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws Exception {
    Config config = ConfigFactory.load().getConfig("xio.testApplication.settings.requestMuxer");
    RequestMuxerLocalConnector connector = new RequestMuxerLocalConnector("test-muxer") {
      @Override
      protected ChannelHandler responseHandler() {
        return null;
      }
      @Override
      public ListenableFuture<Channel> connect() {
        SettableFuture<Channel> result = SettableFuture.create();
        EmbeddedChannel channel = new EmbeddedChannel();
        channels.add(channel);
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
    requestMuxer.start();
  }

  @Test
  public void writeTest() throws Exception {
    Integer payload = new Integer(1);
    RequestMuxer.Request request = requestMuxer.write(payload);
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

  public <T> Optional<T> maybeGetFrom(Future<T> future) {
    try {
      return Optional.ofNullable(Uninterruptibles.getUninterruptibly(future)); // block
    } catch(ExecutionException e) {
      return Optional.empty();
    }
  }

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

  @Test
  public void responseTest() {
    Integer payload = new Integer(1);
    RequestMuxer.Request request = requestMuxer.writeExpectResponse(payload);
    CountDownLatch done = new CountDownLatch(1);
    Optional<UUID> result = maybeGetFrom(request.getWriteFuture());
    assertTrue(result.isPresent());
    result.ifPresent(uuid -> {
      // TODO(CK): write response
      // add callback when the response future comes in
      blockingCallback(request.getResponseFuture(), new FutureCallback<RequestMuxer.Response>() {
        @Override
        public void onSuccess(RequestMuxer.Response response) {
          setSuccess(true);
        }

        @Override
        public void onFailure(Throwable throwable) {
          setFailure(true);
        }
      }); // block
    });
  }

}
