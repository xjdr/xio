package com.xjeffrose.xio.http;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xjeffrose.xio.client.ClientConfig;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.schedulers.Schedulers;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Slf4j
@Ignore // todo: (WK)
public class ClientPoolTest extends Assert {

  @Test
  public void releaseAndAcquire() throws Exception {
    ClientPool pool = new ClientPool(2);
    assertEquals(0, pool.countAvailable());

    IntStream.range(0, 10).forEach(i -> pool.release(mockClient("localhost")));

    assertEquals(2, pool.countAvailable());

    IntStream.range(0, 10).forEach(i -> pool.release(mockClient("google")));

    assertEquals(4, pool.countAvailable());

    IntStream.range(0, 2)
        .forEach(i -> pool.acquire(mockConfig("localhost"), () -> mockClient("localhost")));

    assertEquals(2, pool.countAvailable());

    IntStream.range(0, 2)
        .forEach(i -> pool.acquire(mockConfig("google"), () -> mockClient("localhost")));

    assertEquals(0, pool.countAvailable());
  }

  @Test
  public void releaseAndAcquireThreadSafety() throws Exception {
    // given a client pool
    ClientPool pool = new ClientPool(25);

    // when acquire and release occurs many times on multiple threads
    List<Completable> completables =
        IntStream.range(0, 1000)
            .mapToObj(index -> acquireAndReleaseAsync(pool))
            .collect(Collectors.toList());

    // then an exception is NOT thrown
    Completable.merge(completables).blockingAwait();
  }

  @Test
  public void sameInstance() throws Exception {
    ClientPool pool = new ClientPool(2);
    assertEquals(0, pool.countAvailable());

    Client client = mockClient("localhost");
    IntStream.range(0, 10).forEach(i -> pool.release(client));
    assertEquals(1, pool.countAvailable());

    Client client2 = mockClient("google");
    IntStream.range(0, 10).forEach(i -> pool.release(client2));

    assertEquals(2, pool.countAvailable());
  }

  private Completable acquireAndReleaseAsync(ClientPool pool) {
    return acquireAsync(pool).flatMapCompletable(client -> releaseAsync(pool, client));
  }

  private Observable<Client> acquireAsync(ClientPool pool) {
    return Observable.<Client>create(
            emitter -> {
              Client client = pool.acquire(mockConfig("localhost"), () -> mockClient("localhost"));
              log.debug("acquiring client");
              emitter.onNext(client);
              emitter.onComplete();
            })
        .subscribeOn(Schedulers.io())
        .compose(new RandomInterval<>(100)); // wait randomly up to 100ms
  }

  private Completable releaseAsync(ClientPool pool, Client client) {
    return Completable.create(
            emitter -> {
              log.debug("releasing client");
              pool.release(client);
              emitter.onComplete();
            })
        .subscribeOn(Schedulers.io());
  }

  private Client mockClient(String host) {
    Client client = mock(Client.class);
    when(client.remoteAddress()).thenReturn(new InetSocketAddress(host, 80));
    return client;
  }

  private ClientConfig mockConfig(String host) {
    ClientConfig config = mock(ClientConfig.class);
    when(config.remote()).thenReturn(new InetSocketAddress(host, 80));
    return config;
  }

  private static class RandomInterval<T> implements ObservableTransformer<T, T> {

    private final Random random = new Random();

    private final int maxTimeMs;

    private RandomInterval(int maxTimeMs) {
      this.maxTimeMs = maxTimeMs;
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
      return upstream.flatMap(
          client ->
              Observable.timer(random.nextInt(maxTimeMs), TimeUnit.MILLISECONDS).map(t -> client));
    }
  }
}
