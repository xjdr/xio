package com.xjeffrose.xio.http;

import com.xjeffrose.xio.client.ClientConfig;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientPool {

  private final int maxSize;
  private final Map<InetSocketAddress, Map<Client, Meta>> clientPool;

  public ClientPool(int size) {
    maxSize = size;
    clientPool = new ConcurrentHashMap<>();
  }

  private Map<Client, Meta> getPool(InetSocketAddress address) {
    return clientPool.computeIfAbsent(address, k -> new ConcurrentHashMap<>());
  }

  public void release(Client client) {
    Map<Client, Meta> pool = getPool(client.remoteAddresss());
    if (pool.size() < maxSize && !pool.containsKey(client)) {
      log.warn("releasing client to pool {}", client);
      pool.put(client, new Meta(client));
    } else {
      Meta meta = pool.get(client);
      if (meta != null) {
        log.warn("setting client available in pool {}", client);
        meta.available.set(true);
      }
    }
    log.warn("recycling client {}", client);
    client.recycle();
  }

  public Client acquire(ClientConfig config, Supplier<Client> clientSupplier) {
    return getPool(config.remote())
        .values()
        .stream()
        .filter(
            meta -> {
              boolean available = meta.available.getAndSet(false);
              if (available) {
                int count = meta.usageCount.incrementAndGet();
                log.warn("reusing client in pool with usage count {}", count);
              }
              return available;
            })
        .findFirst()
        .map(mta -> mta.client)
        .orElseGet(clientSupplier);
  }

  private static class Meta {
    final Client client;
    AtomicBoolean available = new AtomicBoolean(true);
    AtomicInteger usageCount = new AtomicInteger(1);

    public Meta(Client client) {
      this.client = client;
    }
  }
}
