package com.xjeffrose.xio.http;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xjeffrose.xio.client.ClientConfig;
import java.net.InetSocketAddress;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.junit.Test;

public class ClientPoolTest extends Assert {

  @Test
  public void releaseAndAcquire() {
    ClientPool pool = new ClientPool(2);
    assertEquals(0, pool.countAvailable());

    IntStream.range(0, 10).forEach(i -> pool.release(mockClient("localhost", 80)));

    assertEquals(2, pool.countAvailable());

    IntStream.range(0, 10).forEach(i -> pool.release(mockClient("google", 80)));

    assertEquals(4, pool.countAvailable());

    IntStream.range(0, 2)
        .forEach(i -> pool.acquire(mockConfig("localhost", 80), () -> mockClient("localhost", 80)));

    assertEquals(2, pool.countAvailable());

    IntStream.range(0, 2)
        .forEach(i -> pool.acquire(mockConfig("google", 80), () -> mockClient("localhost", 80)));

    assertEquals(0, pool.countAvailable());
  }

  @Test
  public void sameInstance() {
    ClientPool pool = new ClientPool(2);
    assertEquals(0, pool.countAvailable());

    Client client = mockClient("localhost", 80);
    IntStream.range(0, 10).forEach(i -> pool.release(client));
    assertEquals(1, pool.countAvailable());

    Client client2 = mockClient("google", 80);
    IntStream.range(0, 10).forEach(i -> pool.release(client2));

    assertEquals(2, pool.countAvailable());
  }

  private Client mockClient(String host, int port) {
    Client client = mock(Client.class);
    when(client.remoteAddresss()).thenReturn(new InetSocketAddress(host, port));
    return client;
  }

  private ClientConfig mockConfig(String host, int port) {
    ClientConfig config = mock(ClientConfig.class);
    when(config.remote()).thenReturn(new InetSocketAddress(host, port));
    return config;
  }
}
